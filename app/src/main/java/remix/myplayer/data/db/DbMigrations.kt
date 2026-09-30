package remix.myplayer.data.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import remix.myplayer.data.db.room.entity.WebDav

internal object DbMigrations {

  val migration3to4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `data` TEXT NOT NULL DEFAULT ''")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `account` TEXT")
      db.execSQL("ALTER TABLE `PlayQueue` ADD COLUMN `pwd` TEXT")

      db.execSQL("CREATE TABLE IF NOT EXISTS `WebDav` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alias` TEXT NOT NULL, `account` TEXT, `pwd` TEXT, `server` TEXT NOT NULL, `lastPath` TEXT, `createAt` INTEGER NOT NULL)")
    }
  }

  val migration4to5 = object : Migration(4, 5) {
    @SuppressLint("Range")
    override fun migrate(db: SupportSQLiteDatabase) {
      val temp = ArrayList<WebDav>()
      val cursor = db.query("select * from `Webdav`")
      while (cursor.moveToNext()) {
        temp.add(
          WebDav(
            cursor.getString(cursor.getColumnIndex("alias")),
            cursor.getString(cursor.getColumnIndex("account")),
            cursor.getString(cursor.getColumnIndex("pwd")),
            cursor.getString(cursor.getColumnIndex("server")),
            cursor.getString(cursor.getColumnIndex("server")),
            cursor.getLong(cursor.getColumnIndex("createAt"))
          ).apply {
            id = cursor.getInt(cursor.getColumnIndex("id"))
          })
      }
      print(temp)
      db.execSQL("DROP TABLE `WebDav`")
      db.execSQL("CREATE TABLE IF NOT EXISTS `WebDav` (`alias` TEXT NOT NULL, `account` TEXT NOT NULL, `pwd` TEXT NOT NULL, `server` TEXT NOT NULL, `lastUrl` TEXT NOT NULL, `createAt` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
      temp.forEach { webDav ->
        db.insert("WebDav", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
          put("alias", webDav.alias)
          put("account", webDav.account)
          put("pwd", webDav.pwd)
          put("server", webDav.server)
          put("lastUrl", webDav.lastUrl)
          put("createAt", webDav.createAt)
          put("id", webDav.id)
        })
      }
    }
  }

  val migration5to6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `MetaDataCache` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `fileSize` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `year` TEXT NOT NULL, `genre` TEXT NOT NULL, `track` TEXT NOT NULL, `updateTime` INTEGER NOT NULL, PRIMARY KEY (`url`))")
    }
  }

  val migration6to7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `Smb` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alias` TEXT NOT NULL, `domain` TEXT, `account` TEXT NOT NULL, `pwd` TEXT NOT NULL, `server` TEXT NOT NULL, `share` TEXT NOT NULL, `lastUrl` TEXT NOT NULL, `createAt` INTEGER NOT NULL)")
    }
  }

  val migration7to8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `play_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `schemaVersion` INTEGER NOT NULL, `eventId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `eventType` TEXT NOT NULL, `canonicalId` TEXT NOT NULL, `audioId` INTEGER, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `listenedMs` INTEGER NOT NULL, `listenRatio` REAL NOT NULL, `playScore` REAL NOT NULL, `completed` INTEGER NOT NULL, `source` TEXT NOT NULL, `endReason` TEXT NOT NULL, `titleSnapshot` TEXT NOT NULL, `artistSnapshot` TEXT NOT NULL, `albumSnapshot` TEXT NOT NULL, `genreSnapshot` TEXT, `sourceUri` TEXT, `contentHash` TEXT, `pathHint` TEXT, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `hour` INTEGER NOT NULL, `weekday` INTEGER NOT NULL)"
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_year` ON `play_events` (`year`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_canonicalId` ON `play_events` (`canonicalId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_audioId` ON `play_events` (`audioId`)")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_source` ON `play_events` (`source`)")

      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_startedAt` ON `play_events` (`startedAt`)")
    }
  }

  val migration8to9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `songId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `artistId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `albumId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `genreId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `playlistId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `mediaType` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `sessionId` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `gapBeforeMs` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `gapAfterMs` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `loopCount` INTEGER NOT NULL DEFAULT 0")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `outputDevice` TEXT")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `isForeground` INTEGER")
      db.execSQL("ALTER TABLE `play_events` ADD COLUMN `decoder` TEXT")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_eventType` ON `play_events` (`eventType`)")
    }
  }
}