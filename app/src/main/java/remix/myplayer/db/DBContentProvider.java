package remix.myplayer.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/12 16:04
 */
public class DBContentProvider extends ContentProvider {
    public static final String AUTHORITY = "remix.myplayer.contentprovider";
    public static final int PLAY_LIST_SINGLE = 1;
    public static final int PLAY_LIST = 2;
    public static final int PLAY_LIST_SONG_SINGLE = 3;
    public static final int PLAY_LIST_SONG = 4;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, "/" + DBOpenHelper.TABLE_PLAY_LIST,PLAY_LIST_SINGLE);
        uriMatcher.addURI(AUTHORITY, "/" + DBOpenHelper.TABLE_PLAY_LIST + "#",PLAY_LIST);
        uriMatcher.addURI(AUTHORITY, "/" + DBOpenHelper.TABLE_PLAY_LIST_SONG,PLAY_LIST_SONG_SINGLE);
        uriMatcher.addURI(AUTHORITY, "/" + DBOpenHelper.TABLE_PLAY_LIST_SONG + "#",PLAY_LIST_SONG);
    }


    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        return null;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    public static final String PLAY_LIST_CONTENT_TYPE = "vnd.android.cursor.dir/play_lists";
    public static final String PLAY_LIST_ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/play_list";

    public static final String PLAY_LIST_SONG_CONTENT_TYPE = "vnd.android.cursor.dir/play_list_songs";
    public static final String PLAY_LIST_SONG_ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/play_list";
    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }
}
