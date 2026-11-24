package remix.myplayer.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.MetaDataCacheDao
import remix.myplayer.data.db.room.entity.MetaDataCache
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SongMetadata
import remix.myplayer.data.remote.RemoteFile
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Owns metadata caching and shared in-flight reads. */
@Singleton
class SongMetadataRepository internal constructor(
  private val metaDataCacheDao: MetaDataCacheDao,
  private val reader: SongMetadataReader,
  private val scope: CoroutineScope,
  private val remoteStat: suspend (Song) -> RemoteFile = { error("Remote stat is not configured") }
) {
  @Inject
  constructor(
    metaDataCacheDao: MetaDataCacheDao, reader: SongMetadataReader,
    sources: MusicSourceRepository
  ) : this(
    metaDataCacheDao, reader, CoroutineScope(Dispatchers.IO + SupervisorJob()), sources::stat
  )

  private class ReadTask(val generation: Long, val size: Long, val dateModified: Long) {
    val result = CompletableDeferred<SongMetadata?>()

    fun matches(song: Song) = size == song.size && dateModified == song.dateModified
  }

  private val jobs = mutableMapOf<SongIdentity, ReadTask>()
  private val generations = mutableMapOf<SongIdentity, Long>()

  // Serializes invalidation, cache writes and ownership of shared tasks.
  private val mutex = Mutex()
  private val _revision = MutableStateFlow(0L)
  val revision = _revision.asStateFlow()

  suspend fun withCachedMetadata(song: Song): Song = withContext(Dispatchers.IO) {
    mutex.withLock {
      val cache = metaDataCacheDao.get(song.getKey())?.takeIf { it.matches(song) }
        ?: return@withLock song
      applyCached(song, cache)
    }
  }

  /** Save directory file versions before songs are added to queues or playlists. No media reads. */
  suspend fun saveRemoteFiles(songs: List<Song>) = withContext(Dispatchers.IO) {
    val remoteSongs = songs.filter { it.isRemote() && (it.size > 0 || it.dateModified > 0) }
      .distinctBy { it.songIdentity }
    if (remoteSongs.isEmpty()) return@withContext
    mutex.withLock {
      val existing = remoteSongs.map { it.getKey() }
        .chunked(CACHE_QUERY_BATCH_SIZE)
        .flatMap { metaDataCacheDao.getAll(it) }
        .associateBy { it.url }
      val changed = remoteSongs.filter { song ->
        existing[song.getKey()]?.matches(song) != true
      }
      if (changed.isEmpty()) return@withLock
      metaDataCacheDao.insertAll(changed.map { song ->
        MetaDataCache(song.getKey(), "", "", "", 0L, song.size, song.dateModified,
          "", "", "", tagsLoaded = false)
      })
      changed.forEach { song ->
        generations[song.songIdentity] = generation(song.songIdentity) + 1
        jobs.remove(song.songIdentity)
      }
      _revision.update { it + 1 }
    }
  }

  /** Resolves cached tags in one DB read per chunk, without changing order or fetching media. */
  suspend fun withCachedMetadata(songs: List<Song>): List<Song> = withContext(Dispatchers.IO) {
    if (songs.isEmpty()) return@withContext emptyList()
    mutex.withLock {
      val caches = songs.map { it.getKey() }.distinct()
        .chunked(CACHE_QUERY_BATCH_SIZE)
        .flatMap { metaDataCacheDao.getAll(it) }
        .associateBy { it.url }
      songs.map { song ->
        caches[song.getKey()]?.takeIf { it.matches(song) }
          ?.let { applyCached(song, it) } ?: song
      }
    }
  }

  /** Shows cached remote tags immediately; checks the file version only when explicitly requested. */
  suspend fun refresh(song: Song): Song {
    if (!song.isRemote()) return getOrFetch(song)
    val previous = withCachedMetadata(song.copy(size = 0L, dateModified = 0L))
      .copy(size = song.size, dateModified = song.dateModified)
    val file = try {
      remoteStat(song)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Unable to stat remote song %s", song.getKey())
      return previous
    }
    if (file.isDirectory) return previous
    // Do not use old cached tags as fallbacks when reading a new file version.
    return getOrFetch(
      song.copy(
        size = file.size,
        dateModified = file.lastModified,
        title = song.displayName.substringBeforeLast('.', song.displayName),
        album = "",
        artist = "",
        duration = 0L,
        year = "",
        genre = "",
        track = null,
        bitRate = "",
        sampleRate = ""
      )
    )
  }

  suspend fun getOrFetch(song: Song): Song = withContext(Dispatchers.IO) {
    // A queue/playlist Song may have no version; recover it before reading or caching tags.
    var resolved = if (song.isRemote()) withCachedMetadata(song) else song
    val key = resolved.songIdentity
    while (true) {
      val (task, isNew) = mutex.withLock {
        jobs[key]?.let { it to false } ?: ReadTask(generation(key), resolved.size, resolved.dateModified).let {
          jobs[key] = it
          it to true
        }
      }
      if (isNew) {
        val toRead = resolved
        scope.launch {
          val result = runCatching { read(toRead, task.generation) }
          withContext(NonCancellable) {
            // Retire ownership before waking waiters, so an immediate retry cannot reuse a failure.
            mutex.withLock { if (jobs[key] === task) jobs.remove(key) }
            result.fold(task.result::complete, task.result::completeExceptionally)
          }
        }
      }
      // A newer file version must not reuse metadata from an older in-flight read.
      if (!task.matches(resolved)) {
        task.result.join()
        resolved = if (song.isRemote()) withCachedMetadata(song) else song
        continue
      }
      val metadata = task.result.await()
      if (mutex.withLock { generation(key) == task.generation }) {
        return@withContext metadata?.applyTo(resolved) ?: resolved
      }
      // Invalidation retired this task; a directory refresh may have supplied a new version.
      resolved = if (song.isRemote()) withCachedMetadata(song) else song
    }
    @Suppress("UNREACHABLE_CODE")
    song
  }

  /** Artwork extraction can fill a missing or outdated version, but cannot overwrite valid tags. */
  suspend fun cacheIfAbsent(song: Song, metadata: SongMetadata): Boolean = withContext(Dispatchers.IO) {
    mutex.withLock {
      val cached = metaDataCacheDao.get(song.getKey())?.takeIf { it.matches(song) }
      if (cached?.tagsLoaded == true) return@withLock false
      val resolved = cached?.let { applyCached(song, it) } ?: song
      metaDataCacheDao.insert(cacheEntry(resolved, metadata))
      _revision.update { it + 1 }
      true
    }
  }

  suspend fun invalidate(key: SongIdentity) = withContext(Dispatchers.IO) {
    mutex.withLock {
      metaDataCacheDao.delete(key.key)
      generations[key] = generation(key) + 1
      jobs.remove(key)
      _revision.update { it + 1 }
    }
  }

  private fun generation(key: SongIdentity): Long = generations[key] ?: 0L

  private fun MetaDataCache.matches(song: Song): Boolean =
    (song.size <= 0 || fileSize == song.size) &&
        (song.dateModified <= 0 || lastModified == song.dateModified)

  private suspend fun validCache(song: Song): MetaDataCache? =
    metaDataCacheDao.get(song.getKey())?.takeIf { it.tagsLoaded && it.matches(song) }

  private fun applyCached(song: Song, cache: MetaDataCache): Song =
    (if (cache.tagsLoaded) cache.metadata().applyTo(song) else song).copy(
      size = song.size.takeIf { it > 0 } ?: cache.fileSize,
      dateModified = song.dateModified.takeIf { it > 0 } ?: cache.lastModified
    )

  private fun cacheEntry(song: Song, metadata: SongMetadata) = MetaDataCache(
    url = song.getKey(), title = metadata.title, artist = metadata.artist, album = metadata.album,
    duration = metadata.duration, fileSize = song.size, lastModified = song.dateModified,
    year = metadata.year, genre = metadata.genre, track = metadata.track.orEmpty(),
    bitRate = metadata.bitRate, sampleRate = metadata.sampleRate
  )

  private suspend fun read(song: Song, expectedGeneration: Long): SongMetadata? {
    val key = song.songIdentity
    mutex.withLock {
      if (generation(key) != expectedGeneration) return null
      validCache(song)?.let { return it.metadata() }
    }
    return try {
      val metadata = reader.read(song)
      mutex.withLock {
        if (generation(key) != expectedGeneration) return null
        // A cover fetch may have cached tags while the active reader was working.
        validCache(song)?.let { return it.metadata() }
        metaDataCacheDao.insert(cacheEntry(song, metadata))
        _revision.update { it + 1 }
      }
      metadata
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      // A transient network/authentication failure is retryable on the next request.
      Timber.w(error, "Unable to read metadata for %s", song.getKey())
      mutex.withLock {
        if (generation(key) == expectedGeneration) validCache(song)?.metadata() else null
      }
    }
  }

  private companion object {
    // Stay below SQLite's 999-variable limit on older Android versions.
    const val CACHE_QUERY_BATCH_SIZE = 500
  }
}
