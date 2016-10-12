package remix.myplayer.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;

import remix.myplayer.util.LogUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/12 16:04
 */
public class DBContentProvider extends ContentProvider {
    public static final String AUTHORITY = "remix.myplayer";
    public static final String CONTENT_AUTHORITY_SLASH = "content://" + AUTHORITY + "/";
    public static final int PLAY_LIST_MULTIPLE = 1;
    public static final int PLAY_LIST_SINGLE = 2;
    public static final int PLAY_LIST_SONG_MULTIPLE = 3;
    public static final int PLAY_LIST_SONG_SINGLE = 4;
    private static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private Context mContext;
    private static ContentObserver mPlayListObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }
    };
    private static ContentObserver mPlayListSongObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }
    };
    static {

        mUriMatcher.addURI(AUTHORITY, "/" + PlayLists.TABLE_NAME, PLAY_LIST_MULTIPLE);
        mUriMatcher.addURI(AUTHORITY, "/" + PlayLists.TABLE_NAME + "#", PLAY_LIST_SINGLE);
        mUriMatcher.addURI(AUTHORITY, "/" + PlayListSongs.TABLE_NAME, PLAY_LIST_SONG_MULTIPLE);
        mUriMatcher.addURI(AUTHORITY, "/" + PlayListSongs.TABLE_NAME + "#", PLAY_LIST_SONG_SINGLE);
    }

    public DBContentProvider(){
        super();
    }
    public DBContentProvider(Context context){

        mContext = context;
    }

    @Override
    public boolean onCreate() {

        if(mContext != null && mContext.getContentResolver() != null){
            mContext.getContentResolver().registerContentObserver(PlayLists.MULTIPLE,true,mPlayListObserver);
            mContext.getContentResolver().registerContentObserver(PlayListSongs.MULTIPLE,true,mPlayListSongObserver);
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getReadableDatabase();
        int match = mUriMatcher.match(uri);
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        //设置表表名
        qBuilder.setTables(match == PLAY_LIST_MULTIPLE || match == PLAY_LIST_SINGLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME);
        Cursor cursor = db.query(match == PLAY_LIST_MULTIPLE || match == PLAY_LIST_SINGLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,
                projection,selection,selectionArgs,null,null,null);
        cursor.setNotificationUri(mContext.getContentResolver(), uri);

        return cursor;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        int match = mUriMatcher.match(uri);
        Uri newUri = Uri.EMPTY;
        if(match == PLAY_LIST_MULTIPLE || match == PLAY_LIST_SONG_MULTIPLE){
            long rowId = db.insert(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,null,values);
            LogUtil.e("DBTest","rowId:" + rowId);
            if(rowId > 0){
                newUri = ContentUris.withAppendedId(match == PLAY_LIST_MULTIPLE ? PlayLists.MULTIPLE : PlayListSongs.MULTIPLE,rowId);
                mContext.getContentResolver().notifyChange(newUri,match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver);
            }
        } else {
            throw new IllegalArgumentException("未知uri:" + uri);
        }
        return newUri;
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
        switch (mUriMatcher.match(uri)){
            case PLAY_LIST_MULTIPLE:
                return PLAY_LIST_CONTENT_TYPE;
            case PLAY_LIST_SINGLE:
                return PLAY_LIST_ENTRY_CONTENT_TYPE;
            case PLAY_LIST_SONG_MULTIPLE:
                return PLAY_LIST_SONG_CONTENT_TYPE;
            case PLAY_LIST_SONG_SINGLE:
                return PLAY_LIST_SONG_ENTRY_CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("未知uri:" + uri);
        }
    }
}
