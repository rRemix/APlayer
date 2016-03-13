package remix.myplayer.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Remix on 2016/3/12.
 */
public class PlayListSQLiteOpenHelper extends SQLiteOpenHelper {
    private final static String TAG = PlayListSQLiteOpenHelper.class.getSimpleName();
    private static String mCreateFavorite = "create table PlayintList (_id ";

    public PlayListSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(null);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
