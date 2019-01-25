package remix.myplayer.db.room

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.content.Context
import android.content.Intent
import remix.myplayer.db.room.AppDatabase.Companion.VERSION
import remix.myplayer.db.room.dao.HistoryDao
import remix.myplayer.db.room.dao.PlayListDao
import remix.myplayer.db.room.dao.PlayQueueDao
import remix.myplayer.db.room.model.History
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.service.MusicService
import remix.myplayer.util.Util.sendLocalBroadcast
import java.util.*

/**
 * Created by remix on 2019/1/12
 */
@Database(entities = [
  PlayList::class,
  PlayQueue::class,
  History::class
], version = VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun playListDao(): PlayListDao

  abstract fun playQueueDao(): PlayQueueDao

  abstract fun historyDao(): HistoryDao

  override fun init(configuration: DatabaseConfiguration) {
    super.init(configuration)
    if (mCallbacks == null) {
      mCallbacks = ArrayList()
    }
    mCallbacks?.add(object : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
//        playListDao().insertPlayList(PlayList(0,DatabaseRepository.MYLOVE,0, LinkedHashSet(),Date().time))

//        db.execSQL(TRIGGER_DELETE_PLAY_LIST)
//        db.execSQL(TRIGGER_DELETE_PLAY_LIST_SONG)
//        db.execSQL(TRIGGER_INSERT_PLAY_LIST_SONG)
      }

    })

  }

  companion object {
    const val VERSION = 1

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

    private fun buildDatabase(context: Context): AppDatabase {
      val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "aplayer.db")
          .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
            }
          })
          .build()
      database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(PlayList.TABLE_NAME) {
        override fun onInvalidated(tables: MutableSet<String>) {
          if (tables.contains(PlayList.TABLE_NAME)) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE))
          }
        }
      })
      return database
    }

    /**
     * select length(audioIds) - length(replace(audioIds, ',','')) from PlayList
     */
    val TRIGGER_UPDATE_PLAYLIST = """
      create trigger playlist_update_trigger
      after delete on PlayList
      begin
      update PlayList set count = 1;
      where id = old.id
      end
    """.trimIndent()

    val TRIGGER_DELETE_PLAY_LIST = "create trigger play_list_delete_trigger " +
        "after delete on PlayList " +
        "begin " +
        "delete from PlayListSong " +
        "where playlist_id = old.id; " +
        "end"
    val TRIGGER_DELETE_PLAY_LIST_SONG = (
        "create trigger play_list_song_delete_trigger " +
            "after delete on PlayListSong " +
            "begin " +
            "update PlayList set count = (select count(*) from PlayListSong where playlist_id = old.playlist_id ) where id = old.playlist_id; " +
            "end")

    val TRIGGER_INSERT_PLAY_LIST_SONG = (
        "create trigger play_list_song_insert_trigger " +
            "after insert on PlayListSong " +
            "begin " +
            "update PlayList set count = (select count(*) from PlayListSong where playlist_id = new.playlist_id ) where id = new.playlist_id; " +
            "end")

  }
}
