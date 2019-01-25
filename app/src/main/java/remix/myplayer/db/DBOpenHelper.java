package remix.myplayer.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @ClassName DBOpenHelper
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/12 09:37
 */
public class DBOpenHelper extends SQLiteOpenHelper {

  public static final String DBNAME = "playlist.db";
  public static final int VERSION = 2;
  public static final String CREATE_TABLE_PLAY_LIST =
      "create table if not exists play_list(" +
          "_id integer primary key ," +
          "name text unique," +
          "count integer," +
          "date integer)";
  public static final String CREATE_TABLE_PLAY_LIST_SONG =
      "create table if not exists play_list_song(" +
          "_id integer primary key," +
          "audio_id integer," +
          "play_list_id integer," +
          "play_list_name text)";
//    "create table if not exists play_list_song(" +
//            "_id integer primary key," +
//            "audio_id integer," +
//            "play_list_id integer," +
//            "_data text," +
//            "artist text," +
//            "artist_id integer," +
//            "album text," +
//            "album_id integer)";

  public static final String CREATE_TRIGGER_DELETE_PLAY_LIST =
      "create trigger play_list_delete_trigger " +
          "after delete on play_list " +
          "begin " +
          "delete from play_list_song where play_list_id = old._id; " +
          "end";
  public static final String CREATE_TRIGGER_DELETE_PLAY_LIST_SONG =
      "create trigger play_list_song_delete_trigger " +
          "after delete on play_list_song " +
          "begin " +
          "update play_list set count = (select count(*) from play_list_song where play_list_id = old.play_list_id ) where _id = old.play_list_id; "
          +
          "end";

  public static final String CREATE_TRIGGER_INSERT_PLAY_LIST_SONG =
      "create trigger play_list_song_add_trigge " +
          "after insert on play_list_song " +
          "begin " +
          "update play_list set count = (select count(*) from play_list_song where play_list_id = new.play_list_id ) where _id = new.play_list_id; "
          +
          "end";


  public DBOpenHelper(Context context) {
    this(context, DBNAME, null, VERSION, dbObj -> {

    });
  }

  private DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
      int version, DatabaseErrorHandler errorHandler) {
    super(context, name, factory, version, errorHandler);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_TABLE_PLAY_LIST);
    db.execSQL(CREATE_TABLE_PLAY_LIST_SONG);
    db.execSQL(CREATE_TRIGGER_DELETE_PLAY_LIST);
    db.execSQL(CREATE_TRIGGER_DELETE_PLAY_LIST_SONG);
    db.execSQL(CREATE_TRIGGER_INSERT_PLAY_LIST_SONG);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (newVersion == 2) {
      db.execSQL("alter table play_list add date integer");
    }
  }
}
