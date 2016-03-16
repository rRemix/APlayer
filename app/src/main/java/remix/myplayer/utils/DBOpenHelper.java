package remix.myplayer.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by taeja on 16-3-16.
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    private static String CREATE_FIRST = "CREATE TABLE IF NOT EXISTS 我的收藏 " +
            "(id integer primary key autoincrement," +
            " songid integer, songname varchar(80))";
    private static final String DATABASE_NAME = "APlayer.db";
    private static final int DATABASE_VERSION = 1;
    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_FIRST);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
