package remix.myplayer.repo

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.room.Room
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.adapter.song
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.entity.MetaDataCache
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.PlaybackRecordPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.remote.RemoteFileSystemFactory
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.helper.SortOrder
import remix.myplayer.data.adapter.AndroidSongMetadataReader
import remix.myplayer.repo.usecase.DeleteSongUseCase
import remix.myplayer.service.PlayQueueStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class SourceSongsTest {
  private lateinit var db: AppDatabase
  private lateinit var settings: SettingPrefs
  private lateinit var sources: MusicSourceRepository
  private lateinit var metadata: SongMetadataRepository
  private lateinit var songs: SongRepository
  private lateinit var library: MediaStoreLibrary
  private lateinit var lists: PlayListRepository
  private lateinit var queue: PlayQueueRepository
  private lateinit var dav: WebDav
  private lateinit var smb: Smb
  private val media = TestMediaProvider()

  @Before
  fun setup() = runBlocking {
    val context = RuntimeEnvironment.getApplication() as Context
    shadowOf(context as Application).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
    settings = SettingPrefs(context).apply { sp.edit().clear().commit() }
    val cover = CoverPrefs(context)
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    sources = MusicSourceRepository(db.webDavDao(), db.smbDao(), RemoteFileSystemFactory())
    metadata = SongMetadataRepository(db.metaDataCacheDao(), AndroidSongMetadataReader(sources), sources)
    library = MediaStoreLibrary(context, settings,
      db.playListDao(), db.playQueueDao(), cover)
    lists = PlayListRepoImpl(db.playListDao(), settings, cover)
    songs = SongRepoImpl(library, sources, metadata, lists, GenreRepoImpl(context, library, settings), settings)
    queue = PlayQueueRepoImpl(songs, db.playQueueDao(), metadata, sources)
    dav = WebDav("DAV", "user", "old-password", "https://dav.test/Music", "https://dav.test/Music")
    dav.id = db.webDavDao().insertOrReplace(dav).toInt()
    smb = Smb("NAS", "WORK", "user", "smb-password", "smb://nas.test", "Music", "smb://nas.test/Music")
    smb.id = db.smbDao().insertOrReplace(smb).toInt()
    media.attachInfo(context, ProviderInfo().apply { authority = "media" })
    ShadowContentResolver.registerProviderInternal("media", media)
  }

  @After fun close() { db.close() }

  @Test
  fun mixedPlaylistAndQueueRestoreInOrderAndRemoteFavoritesUseTheSameIdentity() = runBlocking {
    val favorite = lists.insertPlayList("Favorites")
    assertEquals(1L, favorite)
    val remote = dav.musicSource().song("/Music/歌 %.flac", "歌 %.flac")
    val nas = smb.musicSource().song("album/b.flac", "b.flac")
    val keys = listOf(SongIdentity.local(2), remote.songIdentity, SongIdentity.local(1), nas.songIdentity)
    assertEquals(4, lists.addSongsToPlayList(keys + remote.songIdentity, "Favorites"))
    assertEquals(0, lists.addSongsToPlayList(keys, "Favorites"))
    settings.setPlayListDetailSortOrder(favorite, SortOrder.PLAYLIST_SONG_CUSTOM)
    val playlist = lists.getFavorite()!!
    val loaded = songs.getSongs(listOf(playlist))
    assertEquals(keys, loaded.map { it.songIdentity })
    assertFalse(lists.toggleFavorite(remote.songIdentity))
    assertTrue(lists.toggleFavorite(remote.songIdentity))
    assertTrue(lists.isFavorite(remote.songIdentity))
    queue.replace(loaded)
    assertEquals(keys, queue.getAllSongs().first().map { it.songIdentity })
    settings.lastSong = "https://dav.test/Music/歌 %.flac"
    val store = PlayQueueStore(songs, queue, settings)
    assertEquals(1, withContext(Dispatchers.IO) { store.restore() }.position)
    assertEquals(remote.getKey(), settings.lastSong)
    settings.lastSong = nas.getKey()
    assertEquals(3, withContext(Dispatchers.IO) { store.restore() }.position)
    assertEquals(nas.getKey(), settings.lastSong)
    // A new repository instance has no directory browsing state or in-memory metadata.
    val restartedLibrary = MediaStoreLibrary(RuntimeEnvironment.getApplication(), settings,
      db.playListDao(), db.playQueueDao(), CoverPrefs(RuntimeEnvironment.getApplication()))
    val restarted = SongRepoImpl(restartedLibrary, sources, metadata, lists,
      GenreRepoImpl(RuntimeEnvironment.getApplication(), restartedLibrary, settings), settings)
    assertEquals(keys, restarted.resolveSongs(keys).map { it.songIdentity })
    assertEquals("https://dav.test/Music/%E6%AD%8C%20%25.flac", loaded[1].data)
    assertFalse(loaded.last().data.contains("password"))
  }

  @Test
  fun changingSourceAddressAndPasswordKeepsPlaylistIdentityAndResolvesNewAccessAtOpenTime() = runBlocking {
    val song = dav.musicSource().song("/Music/song.flac", "song.flac")
    lists.insertPlayList("Mix")
    lists.addSongsToPlayList(listOf(song.songIdentity), "Mix")
    val old = sources.resolve(song)
    dav.server = "https://new.test/Library"
    dav.pwd = "new-password"
    db.webDavDao().insertOrReplace(dav)
    val updated = sources.resolve(song)
    assertEquals("https://new.test/Library/song.flac", updated.locator)
    assertNotEquals(old.headers, updated.headers)
    assertEquals(song.songIdentity, lists.allPlayLists().first().single().songIdentities.single())
    assertFalse(song.toString().contains("password"))
  }

  @Test
  fun cachedMetadataRestoresOfflineAndDifferentSourcesDoNotShareIdentityOrMetadata() = runBlocking {
    val first = dav.musicSource().song("/Music/song.flac", "song.flac")
    val secondSource = dav.copy(alias = "Another account", account = "another").apply { id = 0 }
    secondSource.id = db.webDavDao().insertOrReplace(secondSource).toInt()
    val second = secondSource.musicSource().song("/Music/song.flac", "song.flac")
    db.metaDataCacheDao().insert(MetaDataCache(first.getKey(), "Cached title", "Artist", "Album",
      1234, 0, 0, "2026", "Rock", "1", bitRate = "320", sampleRate = "44100"))
    val restored = songs.resolveSongs(listOf(first.songIdentity, second.songIdentity))
    assertEquals("Cached title", restored[0].title)
    assertEquals("320", restored[0].bitRate)
    assertEquals("song", restored[1].title)
    assertNotEquals(first.songIdentity, second.songIdentity)
    val id = lists.insertPlayList("Mix")
    assertEquals(2, lists.addSongsToPlayList(restored.map { it.songIdentity }, "Mix"))
    val snapshot = lists.observePlayList(id).first()!!
    lists.removeSongs(id, setOf(first.songIdentity))
    // Renaming a stale UI value cannot bring a removed entry back or overwrite track ordering.
    lists.updatePlayList(snapshot.copy(name = "Renamed"))
    assertEquals(listOf(second.songIdentity), lists.observePlayList(id).first()!!.songIdentities)
  }

  @Test
  fun remoteMetadataBatchRestoresPlaylistAndQueueWithoutChangingOrder() = runBlocking {
    val first = dav.musicSource().song("/Music/first.flac", "first.flac")
    val second = smb.musicSource().song("album/second.flac", "second.flac")
    db.metaDataCacheDao().insert(MetaDataCache(first.getKey(), "First tags", "A", "Album",
      1_000, 100, 1_000, "2026", "", ""))
    db.metaDataCacheDao().insert(MetaDataCache(second.getKey(), "Second tags", "B", "Album",
      2_000, 200, 2_000, "2026", "", ""))
    val keys = listOf(second.songIdentity, first.songIdentity, second.songIdentity)
    val restored = songs.resolveSongs(keys)
    assertEquals(keys, restored.map { it.songIdentity })
    assertEquals(listOf("Second tags", "First tags", "Second tags"), restored.map { it.title })
    assertEquals(listOf(200L, 100L, 200L), restored.map { it.size })

    val batch = lists.insertPlayList("Batch")
    lists.addSongsToPlayList(keys, "Batch")
    settings.setPlayListDetailSortOrder(batch, SortOrder.PLAYLIST_SONG_CUSTOM)
    assertEquals(listOf("Second tags", "First tags"),
      songs.getSongs(listOf(lists.allPlayLists().first().single())).map { it.title })
    queue.replace(restored)
    assertEquals(listOf("Second tags", "First tags"),
      queue.getAllSongs().first().map { it.title })
  }

  @Test
  fun failedScanPreservesReferencesAndCompleteScanOnlyCleansMissingLocalSongs() = runBlocking {
    val id = lists.insertPlayList("Mix")
    val remote = dav.musicSource().song("/Music/remote.flac", "remote.flac").songIdentity
    lists.addSongsToPlayList(listOf(SongIdentity.local(1), remote, SongIdentity.local(2)), "Mix")
    library.refresh()
    media.ids = listOf(1L)
    media.denied = true
    library.refresh()
    assertEquals(3, lists.observePlayList(id).first()!!.songIdentities.size)
    media.denied = false
    library.refresh()
    assertEquals(listOf(SongIdentity.local(1), remote), lists.observePlayList(id).first()!!.songIdentities)
  }

  @Test
  fun sizeFilterPreservesPlaylistAndQueueReferencesAndRemovingItRestoresSongs() = runBlocking {
    val id = lists.insertPlayList("Keep filtered songs")
    val keys = listOf(SongIdentity.local(1), SongIdentity.local(2))
    lists.addSongsToPlayList(keys, "Keep filtered songs")
    db.playQueueDao().append(keys)
    library.refresh()

    settings.scanSize = 5_000_000
    library.refresh()
    assertTrue(songs.getSongs().isEmpty())
    assertEquals(keys, lists.observePlayList(id).first()!!.songIdentities)
    assertEquals(keys, queue.getIdentities())

    settings.scanSize = 0
    library.refresh()
    assertEquals(keys, songs.getSongs(listOf(lists.observePlayList(id).first()!!)).map { it.songIdentity })
    assertEquals(keys, queue.getAllSongs().first().map { it.songIdentity })
  }

  @Test
  fun failedIdQueryPreservesReferencesWhilePublishingTheNewSnapshot() = runBlocking {
    val id = lists.insertPlayList("Keep references")
    val keys = listOf(SongIdentity.local(1), SongIdentity.local(2))
    lists.addSongsToPlayList(keys, "Keep references")
    db.playQueueDao().append(keys)
    library.refresh()

    media.ids = listOf(1L)
    media.returnNullIds = true
    library.refresh()
    assertEquals(listOf("1"), songs.getSongs().map { it.sourceKey })
    assertEquals(keys, lists.observePlayList(id).first()!!.songIdentities)
    assertEquals(keys, queue.getIdentities())

    media.returnNullIds = false
    library.refresh()
    assertEquals(listOf(keys.first()), lists.observePlayList(id).first()!!.songIdentities)
    assertEquals(listOf(keys.first()), queue.getIdentities())
  }

  @Test
  fun sortingAndAggregatesShareOneSnapshotAndUseMillisecondsAndNumericTrackNumbers() = runBlocking {
    library.refresh()
    val calls = media.calls
    withContext(Dispatchers.IO) {
      AlbumRepoImpl(songs, settings).observeAlbums().first()
      ArtistRepoImpl(songs, settings).observeArtists().first()
      val local = songs.getSongs()
      assertEquals(listOf("1", "2"), ItemsSorter.sortedSongs(local, SortOrder.TRACK_NUMBER).map { it.sourceKey })
      assertEquals(listOf("2", "1"), ItemsSorter.sortedSongs(local, SortOrder.DATE_DESC).map { it.sourceKey })
      assertEquals(2000L, local.single { it.sourceKey == "2" }.dateModified)
    }
    assertEquals(calls, media.calls)
  }

  @Test
  fun reorderKeepsUnavailableReferencesAndLegacyProgressIsUpgraded() = runBlocking {
    val id = lists.insertPlayList("Mix")
    val unavailable = SongIdentity(SourceId("webdav:999"), "offline.flac")
    lists.addSongsToPlayList(listOf(SongIdentity.local(1), unavailable, SongIdentity.local(2)), "Mix")
    lists.updateSongOrder(id, listOf(SongIdentity.local(2), SongIdentity.local(1)))
    assertEquals(listOf(SongIdentity.local(2), unavailable, SongIdentity.local(1)),
      lists.observePlayList(id).first()!!.songIdentities)
    val context = RuntimeEnvironment.getApplication() as Context
    context.getSharedPreferences("PlaybackRecord", Context.MODE_PRIVATE).edit()
      .putString("playlist:$id", "{\"songId\":2,\"positionMs\":4500}").commit()
    val record = PlaybackRecordPrefs(context).getPlaybackRecord("playlist:$id")!!
    assertEquals(SongIdentity.local(2), record.songIdentity)
    assertEquals(4500L, record.positionMs)
  }

  @Test
  fun unavailableSavedQueueIsNotOverwrittenByTheDefaultLocalLibrary() = runBlocking {
    val unavailable = SongIdentity(SourceId("webdav:999"), "offline.flac")
    db.playQueueDao().append(listOf(unavailable))
    val restored = withContext(Dispatchers.IO) { PlayQueueStore(songs, queue, settings).restore() }
    assertTrue(restored.hasSavedQueue)
    assertTrue(restored.songs.isEmpty())
    assertEquals(listOf(unavailable), queue.getIdentities())
  }

  @Test
  fun deletingPlaylistsRetainsTheSourceDeletionSelectionBeforeCascadeRemovesTracks() = runBlocking {
    lists.insertPlayList("Favorites")
    val first = lists.insertPlayList("First")
    val second = lists.insertPlayList("Second")
    lists.addSongsToPlayList(listOf(SongIdentity.local(1), SongIdentity.local(2)), "First")
    lists.addSongsToPlayList(listOf(SongIdentity.local(1)), "Second")
    val models = listOf(lists.observePlayList(first).first()!!, lists.observePlayList(second).first()!!)
    val selected = DeleteSongUseCase(settings, songs, lists, queue)
      .removePlaylistReferences(models, deleteSource = true, parent = null) { error("Not a favorite") }
    assertEquals(setOf(SongIdentity.local(1), SongIdentity.local(2)), selected.map { it.songIdentity }.toSet())
    assertEquals(2, selected.size)
    assertNull(lists.observePlayList(first).first())
    assertNull(lists.observePlayList(second).first())
    assertTrue(db.playListDao().tracks(first).isEmpty())
  }

  @Test
  fun playlistOnlyDeletionDoesNotLoadSongsAndFavoritesAreProtectedFromSourceDeletion() = runBlocking {
    lists.insertPlayList("Favorites")
    val id = lists.insertPlayList("Delete only references")
    lists.addSongsToPlayList(listOf(SongIdentity.local(1)), "Delete only references")
    val model = lists.observePlayList(id).first()!!
    val useCase = DeleteSongUseCase(settings, songs, lists, queue)
    val calls = media.calls
    assertTrue(useCase.removePlaylistReferences(listOf(model), false, null) { error("Not a favorite") }.isEmpty())
    assertEquals(calls, media.calls)
    assertNull(lists.observePlayList(id).first())
    var protected = false
    val favorite = lists.getFavorite()!!
    lists.addSongsToPlayList(listOf(SongIdentity.local(2)), favorite.name)
    assertTrue(useCase.removePlaylistReferences(listOf(lists.getFavorite()!!), true, null) { protected = true }.isEmpty())
    assertTrue(protected)
    assertEquals(listOf(SongIdentity.local(2)), lists.getFavorite()!!.songIdentities)
  }

  @Test
  fun hashCollidingRemoteNamesRemainDistinctInOnePlaylist() = runBlocking {
    assertEquals("Aa".hashCode(), "BB".hashCode())
    val source = dav.musicSource().id
    val first = SongIdentity(source, "Aa")
    val second = SongIdentity(source, "BB")
    lists.insertPlayList("Collisions")
    assertEquals(2, lists.addSongsToPlayList(listOf(first, second), "Collisions"))
    assertNotEquals(first.key, second.key)
  }

  @Test
  fun pathsRoundTripChineseSpacesAndLiteralPercentWithoutCredentials() {
    val root = "https://dav.test/Music%20Library"
    val path = "album/歌 100% #?.flac"
    val url = RemotePaths.resolve(root, path)
    assertEquals(path, RemotePaths.relativePath(root, url))
    assertFalse(url.contains("100% "))
    assertEquals("smb://nas.test/Music/a.flac",
      RemotePaths.withoutCredentials("smb://domain;user:password@nas.test/Music/a.flac"))
  }

  @Test
  fun concurrentInitialReadsPublishLoadedSongsOnceAndAllSubsequentQueriesUseTheSnapshot() = runBlocking {
    val results = (1..8).map { index -> async(Dispatchers.Default) {
      if (index % 2 == 0) songs.getSongs() else songs.observeSongs().first()
    } }.awaitAll()
    assertTrue(results.all { result -> result.map { it.sourceKey } == listOf("1", "2") })
    assertEquals(2, media.calls)
    assertEquals(2, songs.getSongs(SongQuery(text = "Song")).size)
    assertEquals(listOf("2", "1", "2"), songs.resolveSongs(listOf(SongIdentity.local(2),
      SongIdentity.local(999), SongIdentity.local(1), SongIdentity.local(2))).map { it.sourceKey })
    assertEquals(2, media.calls)
  }

  @Test
  fun pathQueriesAreLiteralAndSuffixMatchesReturnAllCandidatesWithoutScanningAgain() = runBlocking {
    media.paths = mapOf(1L to "/music/100%_歌.flac", 2L to "/archive/100%_歌.flac")
    val exact = SongQuery(path = "/music/100%_歌.flac")
    assertEquals(listOf("1"), songs.getSongs(exact).map { it.sourceKey })
    assertEquals(listOf("1"), songs.getSongs(exact.copy(path = "\\music\\100%_歌.flac")).map { it.sourceKey })
    assertEquals(2, songs.getSongs(SongQuery(path = "100%_歌.flac", pathMatch = PathMatch.SUFFIX)).size)
    assertTrue(songs.getSongs(SongQuery(path = "歌.flac", pathMatch = PathMatch.SUFFIX)).isEmpty())
    assertTrue(songs.getSongs(SongQuery(path = "", pathMatch = PathMatch.SUFFIX)).isEmpty())
    assertEquals(2, media.calls)
  }

  @Test
  fun subscriptionsFollowScansAndDetailSortChangesWithoutRescanningForSort() = runBlocking {
    val emissions = Channel<List<Song>>(Channel.UNLIMITED)
    val job = launch(Dispatchers.Default) {
      songs.observeSongs(Album(1L, "Album", 1L, "Artist", 2)).collect { emissions.send(it) }
    }
    try {
      assertEquals(listOf("1", "2"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
      val calls = media.calls
      settings.albumDetailSortOrder = SortOrder.SONG_Z_A
      shadowOf(Looper.getMainLooper()).idle()
      assertEquals(listOf("2", "1"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
      assertEquals(calls, media.calls)
      media.ids = listOf(2L)
      library.refresh()
      assertEquals(listOf("2"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
    } finally { job.cancelAndJoin() }
  }

  @Test
  fun changingGenreDetailSortReordersExistingMembersWithoutQueryingMediaStoreAgain() = runBlocking {
    val emissions = Channel<List<Song>>(Channel.UNLIMITED)
    val job = launch(Dispatchers.Default) {
      songs.observeSongs(Genre(1L, "Rock", 2)).collect { emissions.send(it) }
    }
    try {
      assertEquals(listOf("1", "2"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
      val calls = media.calls
      settings.genreDetailSortOrder = SortOrder.SONG_Z_A
      shadowOf(Looper.getMainLooper()).idle()
      assertEquals(listOf("2", "1"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
      assertEquals(calls, media.calls)
      settings.genreDetailSortOrder = SortOrder.SONG_A_Z
      shadowOf(Looper.getMainLooper()).idle()
      assertEquals(listOf("1", "2"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
      assertEquals(calls, media.calls)
    } finally { job.cancelAndJoin() }
  }

  @Test
  fun nativeGenreMembershipUsesFilteredSnapshotInstancesAndRefreshesWhenSongsAreUnchanged() = runBlocking {
    val genres = GenreRepoImpl(RuntimeEnvironment.getApplication(), library, settings)
    val snapshot = songs.getSongs()
    assertEquals(listOf(2), genres.observeGenres().first().map { it.count })
    val members = genres.getSongs(1L)
    assertEquals(listOf("1", "2"), members.map { it.sourceKey })
    assertSame(snapshot.first(), members.first())
    val emissions = Channel<List<Song>>(Channel.UNLIMITED)
    val job = launch(Dispatchers.Default) { genres.observeSongs(1L).collect { emissions.send(it) } }
    try {
      assertEquals(2, withTimeout(5000) { emissions.receive() }.size)
      media.genreMembers = listOf(2L, 999L)
      library.refresh()
      assertEquals(listOf("2"), withTimeout(5000) { emissions.receive() }.map { it.sourceKey })
    } finally { job.cancelAndJoin() }
  }

  @Test
  fun nullScanKeepsSnapshotAndReferencesWhileSuccessfulEmptyScanRemovesOnlyLocalReferences() = runBlocking {
    val id = lists.insertPlayList("Mix")
    val remote = dav.musicSource().song("remote.flac", "remote.flac").songIdentity
    lists.addSongsToPlayList(listOf(SongIdentity.local(1), remote), "Mix")
    assertEquals(2, songs.getSongs().size)
    media.returnNull = true
    media.ids = emptyList()
    library.refresh()
    assertEquals(2, songs.getSongs().size)
    assertEquals(2, lists.observePlayList(id).first()!!.songIdentities.size)
    media.returnNull = false
    library.refresh()
    assertTrue(songs.getSongs().isEmpty())
    assertEquals(listOf(remote), lists.observePlayList(id).first()!!.songIdentities)
  }

  @Test
  fun revokedPermissionDoesNotQueryTheProviderOrDeleteSavedReferences() = runBlocking {
    val id = lists.insertPlayList("Keep")
    lists.addSongsToPlayList(listOf(SongIdentity.local(1)), "Keep")
    assertEquals(2, songs.getSongs().size)
    val calls = media.calls
    shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
    media.ids = emptyList()
    library.refresh()
    assertEquals(calls, media.calls)
    assertEquals(2, songs.getSongs().size)
    assertEquals(listOf(SongIdentity.local(1)), lists.observePlayList(id).first()!!.songIdentities)
  }

  @Test
  fun playlistActionUsesTheSuppliedSnapshotEvenAfterTheRowHasBeenRemoved() = runBlocking {
    val id = lists.insertPlayList("Snapshot")
    val keys = listOf(SongIdentity.local(2), SongIdentity.local(1))
    lists.addSongsToPlayList(keys, "Snapshot")
    settings.setPlayListDetailSortOrder(id, SortOrder.PLAYLIST_SONG_CUSTOM)
    val saved = lists.observePlayList(id).first()!!
    lists.deletePlayList(id)
    assertEquals(keys, songs.getSongs(listOf(saved)).map { it.songIdentity })
    assertTrue(songs.observeSongs(saved).first().isEmpty())
  }

  class TestMediaProvider : ContentProvider() {
    var ids = listOf(1L, 2L)
    var denied = false
    var returnNull = false
    var returnNullIds = false
    var paths = emptyMap<Long, String>()
    var genreMembers = listOf(1L, 2L, 999L)
    var calls = 0
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? {
      calls++
      if (denied) throw SecurityException("Permission revoked")
      if (returnNull) return null
      if (uri.path?.contains("/genres") == true) {
        return MatrixCursor(requireNotNull(projection)).apply {
          if (uri.lastPathSegment == "members") genreMembers.forEach { addRow(arrayOf(it)) }
          else addRow(arrayOf<Any>(1L, "Rock"))
        }
      }
      val columns = requireNotNull(projection)
      if (returnNullIds && columns.size == 1 && columns[0] == "_id") return null
      val minSize = Regex("""_size > (\d+)""").find(selection.orEmpty())?.groupValues?.get(1)?.toLong()
      val requested = if (selection?.contains("_id IN (") == true || selection?.contains("_id=?") == true)
        args.orEmpty().mapNotNull { it.toLongOrNull() }.toSet() else ids.toSet()
      return MatrixCursor(columns).apply {
        ids.filter { it in requested && (minSize == null || 2_000_000L > minSize) }.forEach { id -> addRow(columns.map<String, Any> { column -> when (column) {
          "_id" -> id
          "title", "_display_name" -> "Song $id"
          "title_key" -> id.toString()
          "_data" -> paths[id] ?: "/music/song$id.flac"
          "album", "artist" -> "Album / Artist"
          "duration" -> 1000L
          "date_modified", "date_added", "track" -> id
          "_size" -> 2_000_000L
          "year" -> 2026
          else -> 1L
        } }.toTypedArray()) }
      }
    }
    override fun getType(uri: Uri) = "audio/flac"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
  }
}
