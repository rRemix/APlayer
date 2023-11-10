package remix.myplayer.db.room

import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.content.Intent
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import remix.myplayer.db.room.AppDatabase.Companion.VERSION
import remix.myplayer.db.room.dao.HistoryDao
import remix.myplayer.db.room.dao.PlayListDao
import remix.myplayer.db.room.dao.PlayQueueDao
import remix.myplayer.db.room.dao.WebDavDao
import remix.myplayer.db.room.model.History
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by remix on 2019/1/12
 */
@Database(entities = [
  PlayList::class,
  PlayQueue::class,
  History::class,
  WebDav::class
], version = VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun playListDao(): PlayListDao

  abstract fun playQueueDao(): PlayQueueDao

  abstract fun historyDao(): HistoryDao

  abstract fun webDavDao(): WebDavDao

  companion object {
    const val VERSION = 4

    @Volatile
    private var INSTANCE: AppDatabase? = null

    @JvmStatic
    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

    private fun buildDatabase(context: Context): AppDatabase {
      val migration3to4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }

      }
      val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "aplayer.db")
          .addMigrations(migration3to4)
          .build()
      database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(PlayList.TABLE_NAME, PlayQueue.TABLE_NAME) {
        override fun onInvalidated(tables: MutableSet<String>) {
          Timber.v("onInvalidated: $tables")
          if (tables.contains(PlayList.TABLE_NAME)) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST, PlayList.TABLE_NAME))
          } else if (tables.contains(PlayQueue.TABLE_NAME)) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST, PlayQueue.TABLE_NAME))
          }
        }
      })
      return database
    }

  }
}
