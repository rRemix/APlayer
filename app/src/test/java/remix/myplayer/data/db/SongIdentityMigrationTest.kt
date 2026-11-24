package remix.myplayer.data.db

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class SongIdentityMigrationTest {
  @get:Rule val folder = TemporaryFolder()

  @Test
  fun upgradePreservesPlaylistsSourcesHistoryAndInterleavedQueueAndValidatesRoomSchema() = runBlocking {
    val context = RuntimeEnvironment.getApplication() as Context
    val file = File(folder.root, "legacy.db")
    createV7(context, file) { db ->
      db.execSQL("INSERT INTO PlayList VALUES (1, 'Favorites', '[2,1,2]', 123)")
      db.execSQL("INSERT INTO PlayList VALUES (20, 'Mix', '[1,2]', 456)")
      db.execSQL("INSERT INTO PlayList VALUES (99, 'Deleted', '[]', 789)")
      db.execSQL("DELETE FROM PlayList WHERE id = 99")
      db.execSQL("INSERT INTO History VALUES (1, 2, 3, 100)")
      db.execSQL("INSERT INTO History VALUES (2, 2, 4, 200)")
      db.execSQL("INSERT INTO WebDav VALUES ('DAV', 'user', 'dav-password', 'https://dav.test/Music', 'https://dav.test/Music', 1, 5)")
      db.execSQL("INSERT INTO PlayQueue VALUES (2, '', '', 1, NULL, NULL)")
      db.execSQL("INSERT INTO PlayQueue VALUES (-123, 'DAV song', 'https://dav.test/Music/歌 %.flac', 2, 'user', 'dav-password')")
      db.execSQL("INSERT INTO PlayQueue VALUES (1, '', '', 3, NULL, NULL)")
    }
    // Opening through Room verifies the v7 -> v8 -> v9 migration chain.
    val room = Room.databaseBuilder(context, AppDatabase::class.java, file.path)
      .addMigrations(SongIdentityMigration, DbMigrations.migration8to9).build()
    try {
      val favorites = room.playListDao().selectById(1)!!.toModel()
      assertEquals(listOf(SongIdentity.local(2), SongIdentity.local(1)), favorites.songIdentities)
      assertEquals(123L, favorites.date)
      assertEquals(listOf(
        SongIdentity.local(2), SongIdentity(SourceId("webdav:5"), "歌 %.flac"),
        SongIdentity.local(1)
      ), room.playQueueDao().get().map { it.identity() })
      assertEquals(7, room.historyDao().find(SourceId.MEDIASTORE.value, "2")!!.play_count)
      assertEquals(200L, room.historyDao().find(SourceId.MEDIASTORE.value, "2")!!.last_play)
      assertEquals("dav-password", room.webDavDao().byId(5)!!.pwd)
      assertTrue(room.playListDao().insert(PlayList(0, "Next", 1)) > 99L)
      room.openHelper.writableDatabase.query("PRAGMA table_info(PlayQueue)").use { cursor ->
        val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        assertEquals(setOf("source_id", "source_key", "position"), columns)
      }
      room.openHelper.writableDatabase.query("PRAGMA table_info(MetaDataCache)").use { cursor ->
        val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        assertTrue("8 -> 9 migration should add tagsLoaded", "tagsLoaded" in columns)
      }
    } finally { room.close() }
  }

  @Test
  fun queuedRemoteWithoutConfigurationIsRecoveredWithoutDiscardingItsCredentials() = runBlocking {
    val context = RuntimeEnvironment.getApplication() as Context
    val file = File(folder.root, "orphan.db")
    createV7(context, file) { db ->
      db.execSQL("INSERT INTO PlayQueue VALUES (-1, 'song', 'https://old.test/song.mp3', 1, 'user', 'password')")
    }
    val room = Room.databaseBuilder(context, AppDatabase::class.java, file.path)
      .addMigrations(SongIdentityMigration, DbMigrations.migration8to9).build()
    try {
      val key = room.playQueueDao().get().single().identity()
      val source = room.webDavDao().byId(key.sourceId.value.substringAfter(':').toInt())!!
      assertEquals("https://old.test", source.server)
      assertEquals("password", source.pwd)
      assertEquals("song.mp3", key.sourceKey)
      assertFalse(key.key.contains("password"))
    } finally { room.close() }
  }

  private fun createV7(context: Context, file: File, seed: (SupportSQLiteDatabase) -> Unit) {
    val schema = JSONObject(requireNotNull(javaClass.getResourceAsStream("/database/v7.json")).bufferedReader().use { it.readText() })
      .getJSONObject("database")
    val helper = FrameworkSQLiteOpenHelperFactory().create(
      SupportSQLiteOpenHelper.Configuration.builder(context).name(file.path)
        .callback(object : SupportSQLiteOpenHelper.Callback(7) {
          override fun onCreate(db: SupportSQLiteDatabase) {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
              val entity = entities.getJSONObject(i)
              val name = entity.getString("tableName")
              db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", name))
              val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
              for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name))
              }
            }
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table VALUES (42, ?)", arrayOf(schema.getString("identityHash")))
            seed(db)
          }
          override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()
    )
    helper.writableDatabase
    helper.close()
  }
}
