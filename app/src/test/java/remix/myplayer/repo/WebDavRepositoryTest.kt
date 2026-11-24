package remix.myplayer.repo

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import remix.myplayer.data.adapter.WebDavDeleteInterceptor
import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.prefs.CoverPrefs
import java.io.IOException
import java.net.SocketTimeoutException
import remix.myplayer.data.db.room.entity.PlayList as PlayListEntity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class WebDavRepositoryTest {
  private lateinit var db: AppDatabase
  private lateinit var covers: CoverPrefs
  private lateinit var repo: WebDavRepository
  private lateinit var source: WebDav
  private lateinit var other: WebDav
  private val liveQueue = mutableListOf<SongIdentity>()
  private var runtimeCleanups = 0

  @Before fun setup() = runBlocking {
    val context = RuntimeEnvironment.getApplication() as Context
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    covers = CoverPrefs(context)
    source = WebDav("DAV", "test", "123", "https://dav.test/Music", "https://dav.test/Music")
    source.id = db.webDavDao().insertOrReplace(source).toInt()
    other = source.copy(alias = "Other").apply { id = 0 }
    other.id = db.webDavDao().insertOrReplace(other).toInt()
    repo = WebDavRepoImpl(db, covers) { matches ->
      runtimeCleanups++
      liveQueue.removeAll(matches)
      Unit
    }
  }

  @After fun close() { db.close() }

  @Test fun deletingSourceRemovesEveryPlaylistAndSavedAndUnsavedQueueReferenceOnlyForThatSource() = runBlocking {
    val first = key("a.flac")
    val second = key("b.flac")
    val local = SongIdentity.local(1)
    val otherKey = key("a.flac", other)
    val favorite = playlist("Favorites", listOf(first, local))
    val mix = playlist("Mix", listOf(otherKey, second, first, local))
    val untouched = playlist("Other", listOf(otherKey, local))
    val unchangedCover = covers.diskRevision("playlist:$untouched")
    db.playQueueDao().append(listOf(local, second, otherKey))
    liveQueue += listOf(first, key("not-saved.flac"), local, otherKey)

    assertEquals(1, repo.delete(source))

    assertNull(db.webDavDao().byId(source.id))
    assertNotNull(db.webDavDao().byId(other.id))
    assertEquals(listOf(local), tracks(favorite))
    assertEquals(listOf(otherKey, local), tracks(mix))
    assertEquals(listOf(local, otherKey), savedQueue())
    assertEquals(listOf(local, otherKey), liveQueue)
    assertEquals(unchangedCover, covers.diskRevision("playlist:$untouched"))
    assertNotEquals("0:0", covers.diskRevision("playlist:$mix"))
  }

  @Test fun deletingFileUsesExactIdentityAndEncodedUrlAndCleansOnlyAfterRemoteSuccess() = runBlocking {
    val target = key("album/歌 100%_#.flac")
    val sibling = key("album/歌 100%_#.flac.bak")
    val otherKey = key(target.sourceKey, other)
    val list = playlist("Mix", listOf(target, sibling, otherKey))
    db.playQueueDao().append(listOf(target, sibling, otherKey))
    liveQueue += listOf(target, sibling, otherKey)
    val client = sardine(204) { requestUrl ->
      assertEquals("https://dav.test/Music/album/%E6%AD%8C%20100%25_%23.flac", requestUrl)
    }

    repo.deleteResource(source, "/Music/${target.sourceKey}", false) { url ->
      assertEquals(listOf(target, sibling, otherKey), tracks(list))
      client.delete(url)
    }

    assertEquals(listOf(sibling, otherKey), tracks(list))
    assertEquals(listOf(sibling, otherKey), savedQueue())
    assertEquals(listOf(sibling, otherKey), liveQueue)
    assertNotNull(db.webDavDao().byId(source.id))
  }

  @Test fun deletingDirectoryIncludesDescendantsButTreatsWildcardsAndPrefixSiblingsLiterally() = runBlocking {
    val inside = listOf(key("album%_/a.flac"), key("album%_/sub/b.flac"))
    val outside = listOf(key("album%_2/c.flac"), key("albumXY/c.flac"), key("album%_/a.flac", other))
    val list = playlist("Mix", inside + outside)
    db.playQueueDao().append(inside + outside)
    liveQueue += inside + outside
    val client = sardine(200) { assertEquals("https://dav.test/Music/album%25_/", it) }

    repo.deleteResource(source, "/Music/album%_/", true) { client.delete(it) }

    assertEquals(outside, tracks(list))
    assertEquals(outside, savedQueue())
    assertEquals(outside, liveQueue)
  }

  @Test fun failedOrPartialDeleteKeepsPlaylistQueueAndSourceForEveryFailure() = runBlocking {
    val keys = listOf(key("album/a.flac"), key("album/sub/b.flac"))
    val list = playlist("Mix", keys)
    db.playQueueDao().append(keys)
    liveQueue += keys
    val cover = covers.diskRevision("playlist:$list")
    for (code in listOf(401, 403, 404, 500, 202, 207)) {
      val client = sardine(code)
      try {
        repo.deleteResource(source, "/Music/album/", true) { client.delete(it) }
        fail("HTTP $code must not be treated as a completed deletion")
      } catch (_: IOException) { }
      assertEquals(keys, tracks(list))
      assertEquals(keys, savedQueue())
      assertEquals(keys, liveQueue)
    }
    try {
      repo.deleteResource(source, "/Music/album/", true) { throw SocketTimeoutException("Timed out") }
      fail("A timeout must preserve references")
    } catch (_: SocketTimeoutException) { }
    assertEquals(keys, tracks(list))
    assertEquals(keys, savedQueue())
    assertEquals(0, runtimeCleanups)
    assertEquals(cover, covers.diskRevision("playlist:$list"))
    assertNotNull(db.webDavDao().byId(source.id))
  }

  @Test fun cancellationAfterConfirmedRemoteDeletionStillCompletesLocalCleanup() = runBlocking {
    val target = key("a.flac")
    val list = playlist("Mix", listOf(target))
    db.playQueueDao().append(listOf(target))
    liveQueue += target
    val job = launch {
      repo.deleteResource(source, "/Music/a.flac", false) { currentCoroutineContext().cancel() }
    }
    job.join()
    assertTrue(tracks(list).isEmpty())
    assertTrue(savedQueue().isEmpty())
    assertTrue(liveQueue.isEmpty())
  }

  @Test fun databaseFailureRollsBackSourceAndReferenceDeletionBeforeTouchingLiveQueue() = runBlocking {
    val target = key("a.flac")
    val list = playlist("Mix", listOf(target))
    db.playQueueDao().append(listOf(target))
    liveQueue += target
    db.openHelper.writableDatabase.execSQL(
      "CREATE TRIGGER reject_source_delete BEFORE DELETE ON WebDav BEGIN SELECT RAISE(ABORT, 'blocked'); END"
    )
    try {
      repo.delete(source)
      fail("The source deletion should fail")
    } catch (_: android.database.sqlite.SQLiteException) { }
    assertNotNull(db.webDavDao().byId(source.id))
    assertEquals(listOf(target), tracks(list))
    assertEquals(listOf(target), savedQueue())
    assertEquals(listOf(target), liveQueue)
    assertEquals(0, runtimeCleanups)
  }

  @Test fun lateBrowseUpdateCannotRecreateDeletedSourceOrOverwriteItsEditedRoot() = runBlocking {
    val updated = source.copy(server = "https://new.test/Library", lastUrl = "https://new.test/Library")
      .apply { id = source.id }
    repo.insertOrReplace(updated)
    repo.updateLastUrl(source, "https://dav.test/Music/old-folder")
    assertEquals(updated.server, db.webDavDao().byId(source.id)!!.server)
    assertEquals(updated.lastUrl, db.webDavDao().byId(source.id)!!.lastUrl)
    repo.updateLastUrl(updated, "https://new.test/Library/Album")
    assertEquals("https://new.test/Library/Album", db.webDavDao().byId(source.id)!!.lastUrl)
    repo.delete(updated)
    repo.updateLastUrl(source, "https://dav.test/Music/old-folder")
    assertNull(db.webDavDao().byId(source.id))
  }

  @Test fun staleFolderCannotDeleteFilesOrReferencesAfterSourceWasEdited() = runBlocking {
    val target = key("a.flac")
    val list = playlist("Mix", listOf(target))
    val updated = source.copy(server = "https://new.test/Library").apply { id = source.id }
    repo.insertOrReplace(updated)
    var remoteCalled = false
    try {
      repo.deleteResource(source, "/Music/a.flac", false) { remoteCalled = true }
      fail("The stale browser must reopen the edited source")
    } catch (_: IOException) { }
    assertFalse(remoteCalled)
    assertEquals(listOf(target), tracks(list))
    assertEquals(0, runtimeCleanups)
  }

  private fun key(path: String, dav: WebDav = source) = SongIdentity(dav.musicSource().id, path)
  private suspend fun playlist(name: String, keys: List<SongIdentity>): Long {
    val id = db.playListDao().insert(PlayListEntity(0, name, 0))
    db.playListDao().addSongs(id, keys)
    return id
  }
  private suspend fun tracks(id: Long) = db.playListDao().selectById(id)!!.toModel().songIdentities
  private suspend fun savedQueue() = db.playQueueDao().get().map { it.identity() }
  private fun sardine(code: Int, onDelete: (String) -> Unit = {}): OkHttpSardine = OkHttpSardine(
    OkHttpClient.Builder().addInterceptor(WebDavDeleteInterceptor).addInterceptor { chain ->
      assertEquals("DELETE", chain.request().method)
      onDelete(chain.request().url.toString())
      Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
        .code(code).message("Test response").body("".toResponseBody()).build()
    }.build()
  )
}
