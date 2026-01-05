package remix.myplayer.repo.usecase

import android.media.MediaMetadataRetriever
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.MetaDataCacheDao
import remix.myplayer.data.db.room.entity.MetaDataCache
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.playback.SmbMediaDataSource
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchMetaDataUseCase @Inject constructor(
  private val metaDataCacheDao: MetaDataCacheDao
) {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val jobs = ConcurrentHashMap<String, Deferred<Unit>>()
  private val lock = Mutex()

  suspend operator fun invoke(song: Song.Remote) = withContext(Dispatchers.IO) {
    if (song.metaFetchState.get() >= 2) return@withContext

    val key = song.data
    val job = lock.withLock {
      jobs[key] ?: scope.async {
        try {
          performFetch(song)
        } finally {
          jobs.remove(key)
        }
      }.also { jobs[key] = it }
    }

    job.await()
    // 尝试读一次缓存，处理可能多个Song实例共享同一个URL的情况
    if (song.metaFetchState.get() == 0) {
      loadFromCache(song)
    }
  }

  private suspend fun loadFromCache(song: Song.Remote): Boolean {
    val cache = metaDataCacheDao.get(song.data)
    if (cache != null) {
      Timber.v("fetchMeta getCache: $cache")
      song.updateMetaData(
        cache.title,
        cache.album,
        cache.artist,
        cache.duration,
        cache.year,
        cache.genre,
        cache.track,
        cache.lastModified
      )
      song.metaFetchState.set(2)
      return true
    }
    return false
  }

  private suspend fun performFetch(song: Song.Remote) {
    Timber.v("fetchMeta performFetch: ${song.data}")

    if (song.metaFetchState.get() >= 2) return
    if (!song.metaFetchState.compareAndSet(0, 1)) {
      return
    }

    if (loadFromCache(song)) {
      return
    }

    val start = System.currentTimeMillis()
    val metadataRetriever = MediaMetadataRetriever()
    try {
      if (song.data.startsWith("smb://") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val smbDataSource = SmbMediaDataSource(song.data)
        metadataRetriever.setDataSource(smbDataSource)
      } else {
        metadataRetriever.setDataSource(song.data, song.headers)
      }
      val title =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
          ?: song.title
      val album =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
      val artist =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
      val duration =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
          ?.toLong() ?: 0L
      val year =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: ""
      val genre =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""
      val track =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS) ?: ""
      val dateModified = if (song.dateModified > 0) {
        song.dateModified
      } else {
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
          ?.toLongOrNull() ?: 0
      }
      song.bitRate =
        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: ""
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        song.sampleRate =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            ?: ""
      }

      song.updateMetaData(
        title,
        album,
        artist,
        duration,
        year,
        genre,
        track,
        dateModified
      )
      song.metaFetchState.set(2)
      metaDataCacheDao.insert(
        MetaDataCache(
          url = song.data,
          title = title,
          artist = artist,
          album = album,
          duration = duration,
          fileSize = song.size,
          lastModified = dateModified,
          year = year,
          genre = genre,
          track = track
        )
      )
    } catch (e: Exception) {
      Timber.v("fetchMeta failed, data: ${song.data} detail: $e")
      song.metaFetchState.set(3)
    } finally {
      Timber.v("fetchMeta spend:${System.currentTimeMillis() - start} ${song.data}")
      try {
        metadataRetriever.release()
      } catch (ignore: Exception) {
      }
    }
  }
}
