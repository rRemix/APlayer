package remix.myplayer.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId

/** In-place upgrade of shipped local/WebDAV data. SMB had no released data to migrate. */
internal object SongIdentityMigration : Migration(7, 8) {
  override fun migrate(db: SupportSQLiteDatabase) {
    val sequence = db.query("SELECT seq FROM sqlite_sequence WHERE name = 'PlayList'").use {
      if (it.moveToFirst()) it.getLong(0) else 0L
    }
    val lists = db.query("SELECT id, name, audioIds, date FROM PlayList").use { cursor ->
      buildList {
        while (cursor.moveToNext()) add(LegacyPlaylist(cursor.getLong(0), cursor.getString(1),
          Json.decodeFromString<List<Long>>(cursor.getString(2)).filter { it > 0 }.distinct(), cursor.getLong(3)))
      }
    }
    // Read queue rows before replacing their schema; resolve remote rows against the retained sources.
    val queue = db.query("SELECT audio_id, data, account, pwd FROM PlayQueue ORDER BY id").use { cursor ->
      buildList {
        while (cursor.moveToNext()) {
          val id = cursor.getLong(0)
          val identity = if (id > 0) SongIdentity.local(id) else webDavIdentity(
            db, cursor.getString(1), cursor.getString(2).orEmpty(), cursor.getString(3).orEmpty()
          )
          if (identity != null) add(identity)
        }
      }.distinct()
    }
    db.execSQL("DROP TABLE PlayList")
    db.execSQL("CREATE TABLE PlayList (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, date INTEGER NOT NULL)")
    db.execSQL("CREATE UNIQUE INDEX index_PlayList_name ON PlayList(name)")
    db.execSQL("CREATE TABLE playlist_tracks (playlist_id INTEGER NOT NULL, source_id TEXT NOT NULL, source_key TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(playlist_id, source_id, source_key), FOREIGN KEY(playlist_id) REFERENCES PlayList(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE UNIQUE INDEX index_playlist_tracks_playlist_id_position ON playlist_tracks(playlist_id, position)")
    lists.forEach { list ->
      db.execSQL("INSERT INTO PlayList(id, name, date) VALUES (?, ?, ?)", arrayOf<Any>(list.id, list.name, list.date))
      list.ids.forEachIndexed { position, id ->
        db.execSQL("INSERT INTO playlist_tracks VALUES (?, ?, ?, ?)",
          arrayOf<Any>(list.id, SourceId.MEDIASTORE.value, id.toString(), position))
      }
    }
    db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = 'PlayList'", arrayOf(sequence))
    db.execSQL("DROP TABLE PlayQueue")
    db.execSQL("CREATE TABLE PlayQueue (source_id TEXT NOT NULL, source_key TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(source_id, source_key))")
    db.execSQL("CREATE UNIQUE INDEX index_PlayQueue_position ON PlayQueue(position)")
    queue.forEachIndexed { position, key ->
      db.execSQL("INSERT INTO PlayQueue VALUES (?, ?, ?)", arrayOf<Any>(key.sourceId.value, key.sourceKey, position))
    }
    db.execSQL("CREATE TABLE History_new (source_id TEXT NOT NULL, source_key TEXT NOT NULL, play_count INTEGER NOT NULL, last_play INTEGER NOT NULL, PRIMARY KEY(source_id, source_key))")
    db.execSQL("INSERT INTO History_new SELECT 'mediastore:external', CAST(audio_id AS TEXT), MIN(SUM(play_count), 2147483647), MAX(last_play) FROM History WHERE audio_id > 0 GROUP BY audio_id")
    db.execSQL("DROP TABLE History")
    db.execSQL("ALTER TABLE History_new RENAME TO History")
    // This is a rebuildable URL-keyed cache. Discard it rather than guessing ownership across accounts.
    db.execSQL("DELETE FROM MetaDataCache")
    db.execSQL("ALTER TABLE MetaDataCache ADD COLUMN bitRate TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE MetaDataCache ADD COLUMN sampleRate TEXT NOT NULL DEFAULT ''")
  }

  private fun webDavIdentity(db: SupportSQLiteDatabase, url: String, account: String, password: String): SongIdentity? {
    val original = Uri.parse(url)
    if (original.scheme !in setOf("http", "https")) return null
    val clean = Uri.parse(RemotePaths.withoutCredentials(url))
    // v7 used the decoded DavResource.path. Preserve literal %, # and ? in filenames.
    val uri = clean.buildUpon()
      .path("/" + url.substringAfter("://").substringAfter('/', "")).build()
    val candidates = mutableListOf<Pair<SourceId, String>>()
    db.query("SELECT id, server, account FROM WebDav").use { cursor ->
      while (cursor.moveToNext()) {
        val root = cursor.getString(1)
        val parsed = Uri.parse(RemotePaths.withoutCredentials(root))
        val rootPath = parsed.path.orEmpty().trimEnd('/')
        val path = uri.path.orEmpty()
        if (parsed.scheme == uri.scheme && parsed.authority == uri.authority &&
          (path == rootPath || path.startsWith("$rootPath/")) && cursor.getString(2) == account) {
          candidates += SourceId("webdav:${cursor.getInt(0)}") to root
        }
      }
    }
    val match = candidates.maxByOrNull { Uri.parse(it.second).path.orEmpty().length }
    if (match != null) return SongIdentity(match.first, RemotePaths.relativePath(match.second, uri.toString()))

    // Retain access to queued WebDAV songs even if the original configuration was removed.
    val root = uri.buildUpon().path("").build().toString().trimEnd('/')
    val values = ContentValues().apply {
      put("alias", uri.host.orEmpty())
      put("account", account)
      put("pwd", password)
      put("createAt", System.currentTimeMillis())
      put("server", root)
      put("lastUrl", root)
    }
    val id = db.insert("WebDav", SQLiteDatabase.CONFLICT_ABORT, values)
    return SongIdentity(SourceId("webdav:$id"), RemotePaths.relativePath(root, uri.toString()))
  }

  private data class LegacyPlaylist(val id: Long, val name: String, val ids: List<Long>, val date: Long)
}
