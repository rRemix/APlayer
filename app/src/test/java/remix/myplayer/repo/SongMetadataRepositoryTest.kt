package remix.myplayer.repo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import remix.myplayer.data.db.room.dao.MetaDataCacheDao
import remix.myplayer.data.db.room.entity.MetaDataCache
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongMetadata
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.remote.RemoteFile
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class SongMetadataRepositoryTest {
  private val song = Song(SourceId("webdav:1"), "song.flac", "https://music.test/song.flac", "song.flac")

  @Test
  fun directoryVersionIsRestoredBeforeTagsAreReadAndThenReused() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val calls = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      calls.incrementAndGet()
      metadata("Extracted")
    }, CoroutineScope(owner + Dispatchers.IO))
    val listed = song.copy(size = 1234, dateModified = 5678)
    try {
      repository.saveRemoteFiles(listOf(listed))
      val partial = cache.get(song.getKey())!!
      assertFalse(partial.tagsLoaded)
      assertEquals(1234L, partial.fileSize)
      assertEquals("song.flac", repository.withCachedMetadata(song).title)
      assertEquals(1234L, repository.withCachedMetadata(song).size)
      assertEquals(0, calls.get())

      val resolved = repository.getOrFetch(song) // Simulate a Song restored from a playlist.
      assertEquals("Extracted", resolved.title)
      assertEquals(1234L, resolved.size)
      assertEquals(5678L, resolved.dateModified)
      assertTrue(cache.get(song.getKey())!!.tagsLoaded)
      assertEquals(1, calls.get())
      assertEquals("Extracted", repository.getOrFetch(song).title)
      assertEquals(1, calls.get())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun reListingUnchangedFileDoesNotDiscardPreviouslyReadTags() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      reads.incrementAndGet()
      metadata("Original")
    }, CoroutineScope(owner + Dispatchers.IO))
    val version = song.copy(size = 1234, dateModified = 5678)
    try {
      repository.saveRemoteFiles(listOf(version))
      assertEquals("Original", repository.getOrFetch(song).title)
      val revision = repository.revision.value
      repository.saveRemoteFiles(listOf(version, version))
      assertEquals(revision, repository.revision.value)
      assertTrue(cache.get(song.getKey())!!.tagsLoaded)
      assertEquals("Original", repository.getOrFetch(song).title)
      assertEquals(1, reads.get())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun reListingChangedFileDropsOldTagsButKeepsNewFileVersion() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      metadata(if (reads.incrementAndGet() == 1) "Original" else "Updated")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      repository.saveRemoteFiles(listOf(song.copy(size = 100, dateModified = 1000)))
      assertEquals("Original", repository.getOrFetch(song).title)
      repository.saveRemoteFiles(listOf(song.copy(size = 200, dateModified = 2000)))
      assertFalse(cache.get(song.getKey())!!.tagsLoaded)
      val partial = repository.withCachedMetadata(song)
      assertEquals("song.flac", partial.title)
      assertEquals(200L, partial.size)
      assertEquals(2000L, partial.dateModified)
      assertEquals("Updated", repository.getOrFetch(song).title)
      assertEquals(2, reads.get())
      assertTrue(cache.get(song.getKey())!!.tagsLoaded)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun coverTagsCanCompleteFileOnlyCacheAndUnknownDirectoryDataDoesNotEraseIt() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      reads.incrementAndGet()
      metadata("Unexpected")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      repository.saveRemoteFiles(listOf(song.copy(size = 100, dateModified = 200)))
      assertTrue(repository.cacheIfAbsent(song, metadata("Cover tags")))
      assertTrue(cache.get(song.getKey())!!.tagsLoaded)
      assertEquals(100L, cache.get(song.getKey())!!.fileSize)
      assertEquals(200L, cache.get(song.getKey())!!.lastModified)
      repository.saveRemoteFiles(listOf(song)) // Server omitted attributes.
      assertEquals("Cover tags", repository.getOrFetch(song).title)
      assertFalse(repository.cacheIfAbsent(song, metadata("Duplicate")))
      assertEquals(0, reads.get())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun directoryRefreshDuringReadRetriesUsingTheNewVersion() = runBlocking {
    withTimeout(5_000) {
      val owner = SupervisorJob()
      val cache = MemoryCache()
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val calls = AtomicInteger()
      val repository = SongMetadataRepository(cache, SongMetadataReader { input ->
        if (calls.incrementAndGet() == 1) {
          started.complete(Unit)
          release.await()
          metadata("Stale")
        } else {
          assertEquals(200L, input.size)
          metadata("Fresh")
        }
      }, CoroutineScope(owner + Dispatchers.IO))
      try {
        val pending = async { repository.getOrFetch(song) }
        started.await()
        repository.saveRemoteFiles(listOf(song.copy(size = 200, dateModified = 2000)))
        release.complete(Unit)
        assertEquals("Fresh", pending.await().title)
        assertEquals(200L, cache.get(song.getKey())!!.fileSize)
        assertEquals("Fresh", cache.get(song.getKey())!!.title)
        assertEquals(2, calls.get())
      } finally { owner.cancelAndJoin() }
    }
  }

  @Test
  fun invalidationRetiresOldReadAndAllWaitersReceiveFreshMetadata() = runBlocking {
    withTimeout(5_000) {
      val owner = SupervisorJob()
      val cache = MemoryCache()
      val calls = AtomicInteger()
      val oldStarted = CompletableDeferred<Unit>()
      val releaseOld = CompletableDeferred<Unit>()
      val freshStarted = CompletableDeferred<Unit>()
      val releaseFresh = CompletableDeferred<Unit>()
      val reader = SongMetadataReader {
        when (calls.incrementAndGet()) {
          1 -> { oldStarted.complete(Unit); releaseOld.await(); metadata("Old tags") }
          2 -> { freshStarted.complete(Unit); releaseFresh.await(); metadata("Fresh tags") }
          else -> error("The active fresh read should be shared")
        }
      }
      val useCase = SongMetadataRepository(cache, reader, CoroutineScope(owner + Dispatchers.IO))
      try {
        val oldWaiter = async { useCase.getOrFetch(song) }
        oldStarted.await()
        useCase.invalidate(song.songIdentity)
        val freshWaiter = async { useCase.getOrFetch(song) }
        // A request after invalidation must start independently of the blocked old task.
        freshStarted.await()
        releaseOld.complete(Unit)
        releaseFresh.complete(Unit)
        assertEquals("Fresh tags", oldWaiter.await().title)
        assertEquals("Fresh tags", freshWaiter.await().title)
        assertEquals("Fresh tags", cache.get(song.getKey())!!.title)
        assertEquals(listOf("Fresh tags"), cache.publishedTitles)
        assertEquals(2, calls.get())
      } finally { owner.cancelAndJoin() }
    }
  }

  @Test
  fun differentFileVersionsDoNotShareAnInFlightRead() = runBlocking {
    withTimeout(5_000) {
      val owner = SupervisorJob()
      val cache = MemoryCache()
      val calls = AtomicInteger()
      val oldStarted = CompletableDeferred<Unit>()
      val releaseOld = CompletableDeferred<Unit>()
      val original = song.copy(size = 100, dateModified = 1_000)
      val updated = song.copy(size = 200, dateModified = 2_000)
      val repository = SongMetadataRepository(cache, SongMetadataReader {
        when (calls.incrementAndGet()) {
          1 -> { oldStarted.complete(Unit); releaseOld.await(); metadata("Old tags") }
          2 -> metadata("Updated tags")
          else -> error("The new file version should only be read once")
        }
      }, CoroutineScope(owner + Dispatchers.IO))
      try {
        val oldWaiter = async { repository.getOrFetch(original) }
        oldStarted.await()
        // Start undispatched on IO so it joins the old task before that task is released.
        val updatedWaiter = async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
          repository.getOrFetch(updated)
        }
        releaseOld.complete(Unit)
        assertEquals("Old tags", oldWaiter.await().title)
        assertEquals("Updated tags", updatedWaiter.await().title)
        assertEquals(2, calls.get())
        assertEquals("Updated tags", cache.get(song.getKey())!!.title)
        assertEquals(updated.size, cache.get(song.getKey())!!.fileSize)
        assertEquals(updated.dateModified, cache.get(song.getKey())!!.lastModified)
      } finally { owner.cancelAndJoin() }
    }
  }

  @Test
  fun completedCacheIsRemovedAndTheNextRequestReadsAgain() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val calls = AtomicInteger()
    val useCase = SongMetadataRepository(cache, SongMetadataReader {
      metadata("Read ${calls.incrementAndGet()}")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      assertEquals("Read 1", useCase.getOrFetch(song).title)
      assertEquals("Read 1", useCase.getOrFetch(song).title)
      useCase.invalidate(song.songIdentity)
      assertNull(cache.get(song.getKey()))
      assertEquals("Read 2", useCase.getOrFetch(song).title)
      assertEquals(2, calls.get())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun cancellingOneWaiterDoesNotCancelTheSharedRead() = runBlocking {
    withTimeout(5_000) {
      val owner = SupervisorJob()
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val calls = AtomicInteger()
      val useCase = SongMetadataRepository(MemoryCache(), SongMetadataReader {
        calls.incrementAndGet()
        started.complete(Unit)
        release.await()
        metadata("Shared tags")
      }, CoroutineScope(owner + Dispatchers.IO))
      try {
        val cancelled = async { useCase.getOrFetch(song) }
        started.await()
        cancelled.cancelAndJoin()
        val remaining = async { useCase.getOrFetch(song) }
        release.complete(Unit)
        assertEquals("Shared tags", remaining.await().title)
        assertEquals(1, calls.get())
      } finally { owner.cancelAndJoin() }
    }
  }

  @Test
  fun failedReadCanBeRetried() = runBlocking {
    val owner = SupervisorJob()
    val calls = AtomicInteger()
    val useCase = SongMetadataRepository(MemoryCache(), SongMetadataReader {
      if (calls.incrementAndGet() == 1) throw IOException("Offline")
      metadata("Retried tags")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      assertEquals(song, useCase.getOrFetch(song))
      assertEquals("Retried tags", useCase.getOrFetch(song).title)
      assertEquals(2, calls.get())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun extractedMetadataIsWithCachedMetadataWithoutCallingReader() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val calls = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      calls.incrementAndGet()
      metadata("Reader tags")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      assertTrue(repository.cacheIfAbsent(song, metadata("Embedded tags")))
      assertEquals("Embedded tags", repository.withCachedMetadata(song).title)
      assertEquals("Embedded tags", repository.getOrFetch(song).title)
      assertFalse(repository.cacheIfAbsent(song, metadata("Duplicate tags")))
      assertEquals(0, calls.get())
      assertEquals(1L, repository.revision.value)
      assertEquals(listOf("Embedded tags"), cache.publishedTitles)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun extractedMetadataReplacesCacheForDifferentFileVersion() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      error("Extracted metadata should already be cached")
    }, CoroutineScope(owner + Dispatchers.IO))
    val original = song.copy(size = 100, dateModified = 1_000)
    val updated = song.copy(size = 200, dateModified = 2_000)
    try {
      assertTrue(repository.cacheIfAbsent(original, metadata("Old tags")))
      assertTrue(repository.cacheIfAbsent(updated, metadata("Updated tags")))
      assertEquals("Updated tags", repository.getOrFetch(updated).title)
      assertEquals(updated.size, cache.get(song.getKey())!!.fileSize)
      assertEquals(updated.dateModified, cache.get(song.getKey())!!.lastModified)
      assertEquals(2L, repository.revision.value)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun withCachedMetadataAllPreservesOrderDuplicatesAndFileVersionChecks() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      error("Reading cached songs must not open media")
    }, CoroutineScope(owner + Dispatchers.IO))
    val original = song.copy(size = 100, dateModified = 1_000)
    val second = song.copy(sourceKey = "second.flac", displayName = "second.flac")
    val missing = song.copy(sourceKey = "missing.flac", displayName = "missing.flac")
    try {
      cache.insert(MetaDataCache(original.getKey(), "First", "A", "Album", 1_000,
        100, 1_000, "2026", "", ""))
      cache.insert(MetaDataCache(second.getKey(), "Second", "B", "Album", 2_000,
        200, 2_000, "2026", "", ""))
      val knownOtherVersion = original.copy(size = 101, dateModified = 2_000)
      val input = listOf(original, second, original, knownOtherVersion, missing)
      val result = repository.withCachedMetadata(input)

      assertEquals(listOf("First", "Second", "First", "song.flac", "song.flac"),
        result.map { it.title })
      assertEquals(listOf(100L, 200L, 100L, 101L, 0L), result.map { it.size })
      assertEquals(listOf(1_000L, 2_000L, 1_000L, 2_000L, 0L),
        result.map { it.dateModified })
      assertEquals(0, cache.singleReads.get())
      assertEquals(listOf(3), cache.batchSizes.toList())
      assertEquals(input.map { it.songIdentity }, result.map { it.songIdentity })
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun withCachedMetadataAllSplitsLargeListsIntoBoundedQueries() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      error("A cache lookup must not read media")
    }, CoroutineScope(owner + Dispatchers.IO))
    val songs = (1..1_001).map { song.copy(sourceKey = "song-$it.flac") }
    try {
      assertEquals(songs, repository.withCachedMetadata(songs))
      assertEquals(listOf(500, 500, 1), cache.batchSizes.toList())
      assertEquals(0, cache.singleReads.get())
      assertTrue(repository.withCachedMetadata(emptyList()).isEmpty())
      assertEquals(listOf(500, 500, 1), cache.batchSizes.toList())
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun extractedMetadataWinsOverAlreadyRunningRead() = runBlocking {
    withTimeout(5_000) {
      val owner = SupervisorJob()
      val cache = MemoryCache()
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val calls = AtomicInteger()
      val repository = SongMetadataRepository(cache, SongMetadataReader {
        calls.incrementAndGet()
        started.complete(Unit)
        release.await()
        metadata("Late reader")
      }, CoroutineScope(owner + Dispatchers.IO))
      try {
        val waiter = async { repository.getOrFetch(song) }
        started.await()
        assertTrue(repository.cacheIfAbsent(song, metadata("Embedded tags")))
        release.complete(Unit)
        assertEquals("Embedded tags", waiter.await().title)
        assertEquals("Embedded tags", cache.get(song.getKey())!!.title)
        assertEquals(listOf("Embedded tags"), cache.publishedTitles)
        assertEquals(1, calls.get())
      } finally { owner.cancelAndJoin() }
    }
  }

  @Test
  fun validWithCachedMetadataResultIsNotOverwrittenByExtractedMetadata() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      metadata("Reader tags")
    }, CoroutineScope(owner + Dispatchers.IO))
    try {
      assertEquals("Reader tags", repository.getOrFetch(song).title)
      assertFalse(repository.cacheIfAbsent(song, metadata("Late embedded")))
      assertEquals("Reader tags", repository.withCachedMetadata(song).title)
      assertEquals(1L, repository.revision.value)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun remoteRefreshUsesPathCacheWhenVersionHasNotChanged() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val stats = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      reads.incrementAndGet()
      metadata("Unexpected read")
    }, CoroutineScope(owner + Dispatchers.IO), {
      stats.incrementAndGet()
      RemoteFile("song.flac", "song.flac", false, 100, 1_000)
    })
    try {
      repository.cacheIfAbsent(song.copy(size = 100, dateModified = 1_000), metadata("Original"))
      val result = repository.refresh(song) // Like a queue-restored Song: no version info.
      assertEquals("Original", result.title)
      assertEquals(100L, result.size)
      assertEquals(1_000L, result.dateModified)
      assertEquals(0, reads.get())
      assertEquals(1, stats.get())
      assertEquals(1L, repository.revision.value)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun remoteRefreshReadsNewTagsOnlyAfterFileVersionChanges() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      reads.incrementAndGet()
      metadata("Updated")
    }, CoroutineScope(owner + Dispatchers.IO), {
      RemoteFile("song.flac", "song.flac", false, 200, 2_000)
    })
    try {
      repository.cacheIfAbsent(song.copy(size = 100, dateModified = 1_000), metadata("Original"))
      val result = repository.refresh(song)
      assertEquals("Updated", result.title)
      assertEquals(200L, result.size)
      assertEquals(2_000L, result.dateModified)
      assertEquals(200L, cache.get(song.getKey())!!.fileSize)
      assertEquals(2_000L, cache.get(song.getKey())!!.lastModified)
      assertEquals(1, reads.get())
      assertEquals(2L, repository.revision.value)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun remoteRefreshDoesNotInheritOldTagsWhenNewFileOmitsThem() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    // Mimics MediaMetadataExtractor's fallback to properties of the supplied Song.
    val repository = SongMetadataRepository(cache, SongMetadataReader { input ->
      SongMetadata(input.title, input.album, input.artist, input.duration,
        input.year, input.genre, input.track, input.bitRate, input.sampleRate)
    }, CoroutineScope(owner + Dispatchers.IO), {
      RemoteFile("song.flac", "song.flac", false, 200, 2_000)
    })
    try {
      repository.cacheIfAbsent(song.copy(size = 100, dateModified = 1_000),
        SongMetadata("Old title", "Old album", "Old artist", 1_000,
          "2025", "Old genre", "1", "320", "44100"))
      val result = repository.refresh(song.copy(title = "Caller old title",
        album = "Caller old album", artist = "Caller old artist"))
      assertEquals("song", result.title)
      assertEquals("", result.album)
      assertEquals("", result.artist)
      assertEquals(0L, result.duration)
      assertEquals("", result.year)
      assertEquals("", result.genre)
      assertNull(result.track)
      assertEquals("", result.bitRate)
      assertEquals("", result.sampleRate)
      assertEquals(200L, result.size)
      assertEquals(2_000L, result.dateModified)
      assertEquals("", cache.get(song.getKey())!!.album)
      assertEquals("", cache.get(song.getKey())!!.artist)
    } finally { owner.cancelAndJoin() }
  }

  @Test
  fun remoteStatFailureReturnsPathCacheWithoutOverwritingIt() = runBlocking {
    val owner = SupervisorJob()
    val cache = MemoryCache()
    val reads = AtomicInteger()
    val repository = SongMetadataRepository(cache, SongMetadataReader {
      reads.incrementAndGet()
      metadata("Unexpected read")
    }, CoroutineScope(owner + Dispatchers.IO), { throw IOException("Offline") })
    try {
      repository.cacheIfAbsent(song.copy(size = 100, dateModified = 1_000), metadata("Original"))
      val result = repository.refresh(song)
      assertEquals("Original", result.title)
      assertEquals(0, reads.get())
      assertEquals(100L, cache.get(song.getKey())!!.fileSize)
      assertEquals(1L, repository.revision.value)
    } finally { owner.cancelAndJoin() }
  }

  private fun metadata(title: String) = SongMetadata(title, "Album", "Artist", 1000, "2026", "", "1")

  private class MemoryCache : MetaDataCacheDao {
    private val entries = ConcurrentHashMap<String, MetaDataCache>()
    val publishedTitles = CopyOnWriteArrayList<String>()
    val batchSizes = CopyOnWriteArrayList<Int>()
    val singleReads = AtomicInteger()
    override suspend fun get(url: String): MetaDataCache? {
      singleReads.incrementAndGet()
      return entries[url]
    }
    override suspend fun getAll(urls: List<String>): List<MetaDataCache> {
      batchSizes += urls.size
      return urls.mapNotNull(entries::get)
    }
    override suspend fun insert(cache: MetaDataCache) { entries[cache.url] = cache; publishedTitles += cache.title }
    override suspend fun insertAll(caches: List<MetaDataCache>) { caches.forEach { insert(it) } }
    override suspend fun delete(key: String) { entries.remove(key) }
  }
}
