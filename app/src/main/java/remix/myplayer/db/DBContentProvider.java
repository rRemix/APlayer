package remix.myplayer.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.ArrayList;

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
//    public static final int PLAY_LIST_SINGLE = 2;
    public static final int PLAY_LIST_SONG_MULTIPLE = 3;
//    public static final int PLAY_LIST_SONG_SINGLE = 4;
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
            return super.deliverSelfNotifications();
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
            return super.deliverSelfNotifications();
        }
    };
    static {
        mUriMatcher.addURI(AUTHORITY, "/" + PlayLists.TABLE_NAME, PLAY_LIST_MULTIPLE);
//        mUriMatcher.addURI(AUTHORITY, "/" + PlayLists.TABLE_NAME + "#", PLAY_LIST_SINGLE);
        mUriMatcher.addURI(AUTHORITY, "/" + PlayListSongs.TABLE_NAME, PLAY_LIST_SONG_MULTIPLE);
//        mUriMatcher.addURI(AUTHORITY, "/" + PlayListSongs.TABLE_NAME + "#", PLAY_LIST_SONG_SINGLE);
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
//            mContext.getContentResolver().registerContentObserver(PlayLists.CONTENT_URI,true,mPlayListObserver);
//            mContext.getContentResolver().registerContentObserver(PlayListSongs.CONTENT_URI,true,mPlayListSongObserver);
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getReadableDatabase();
        int match = mUriMatcher.match(uri);
        Cursor cursor = db.query(match == PLAY_LIST_MULTIPLE  ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,
                projection,selection,selectionArgs,null,null,null);
//        cursor.setNotificationUri(mContext.getContentResolver(), uri);

        if(db != null)
            db.close();
        return cursor;
    }

    /**
     *  插入多条歌曲信息
     * @return
     */
    public int insertMultiSong(ArrayList<PlayListSongInfo> songs){
        if(songs == null || songs.size() == 0)
            return 0;
        int lines = 0;
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        for(PlayListSongInfo info : songs){
            try {
                ContentValues cv = new ContentValues();
                cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID,info.AudioId);
                cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID,info.PlayListID);
                cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME,info.PlayListName);
                if(db.insert(PlayListSongs.TABLE_NAME,null,cv) > 0){
                    lines++;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
//        try {
//            synchronized (DBOpenHelper.getInstance()){
//                db.beginTransaction();
//                for(PlayListSongInfo info : songs){
//                    ContentValues cv = new ContentValues();
//                    cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID,info.AudioId);
//                    cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID,info.PlayListID);
//                    cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME,info.PlayListName);
//                    try {
//                        if(db.insert(PlayListSongs.TABLE_NAME,null,cv) > 0){
//                            lines++;
//                        }
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//
//                }
//                db.setTransactionSuccessful();
//                db.endTransaction();
//                mContext.getContentResolver().notifyChange(PlayListSongs.CONTENT_URI,null);
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        } finally {
//            if(db != null)
//                db.close();
//        }

        return lines;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        int match = mUriMatcher.match(uri);
        Uri newUri = Uri.EMPTY;
        try {
            synchronized (DBOpenHelper.getInstance()){
                if(match == PLAY_LIST_MULTIPLE || match == PLAY_LIST_SONG_MULTIPLE){
                    long rowId = db.insert(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,null,values);
                    LogUtil.d("DBTest","rowId:" + rowId);
                    if(rowId > 0){
                        newUri = ContentUris.withAppendedId(match == PLAY_LIST_MULTIPLE ? PlayLists.CONTENT_URI : PlayListSongs.CONTENT_URI,rowId);
                        mContext.getContentResolver().notifyChange(newUri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(db != null)
                db.close();
        }
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        int match = mUriMatcher.match(uri);
        int deleteRow = 0;
        try {
            synchronized (DBOpenHelper.getInstance()){
                deleteRow = db.delete(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME, selection,selectionArgs);
                mContext.getContentResolver().notifyChange(uri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if(db != null)
                db.close();
        }

        return deleteRow;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = DBOpenHelper.getInstance().getWritableDatabase();
        int match = mUriMatcher.match(uri);
        int updateRow = 0;
        try {
            synchronized (DBOpenHelper.getInstance()){
                updateRow = db.delete(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,
                        selection,selectionArgs);
                mContext.getContentResolver().notifyChange(uri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(db != null)
                db.close();
        }

        return updateRow;
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
//            case PLAY_LIST_SINGLE:
//                return PLAY_LIST_ENTRY_CONTENT_TYPE;
            case PLAY_LIST_SONG_MULTIPLE:
                return PLAY_LIST_SONG_CONTENT_TYPE;
//            case PLAY_LIST_SONG_SINGLE:
//                return PLAY_LIST_SONG_ENTRY_CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("未知uri:" + uri);
        }
    }
}
