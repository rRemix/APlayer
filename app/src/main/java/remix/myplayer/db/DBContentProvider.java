package remix.myplayer.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import remix.myplayer.application.APlayerApplication;
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
    public static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI(AUTHORITY,PlayLists.TABLE_NAME, PLAY_LIST_MULTIPLE);
        mUriMatcher.addURI(AUTHORITY,PlayLists.TABLE_NAME + "/#", PLAY_LIST_SINGLE);
        mUriMatcher.addURI(AUTHORITY,PlayListSongs.TABLE_NAME, PLAY_LIST_SONG_MULTIPLE);
        mUriMatcher.addURI(AUTHORITY,PlayListSongs.TABLE_NAME + "/#", PLAY_LIST_SONG_SINGLE);
    }

    @Override
    public boolean onCreate() {
        return true;
    }


    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        SQLiteDatabase db = DBManager.getInstance().openDataBase();
        try {
            int match = mUriMatcher.match(uri);
            cursor = db.query(match == PLAY_LIST_MULTIPLE  ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,
                        projection,selection,selectionArgs,null,null,null);
            cursor.setNotificationUri(APlayerApplication.getContext().getContentResolver(), Uri.parse(CONTENT_AUTHORITY_SLASH));
        } catch (Exception e){
            e.printStackTrace();
        }
        return cursor;
    }


    /**
     *  插入多条歌曲信息
     * @return
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if(uri == null || values == null || values.length == 0)
            return 0;
        SQLiteDatabase db = DBManager.getInstance().openDataBase();
        int match = mUriMatcher.match(uri);
        int lines = 0;
        try {
            db.beginTransaction(); //开始事务
            //数据库操作
            for (ContentValues cv : values) {
                db.insert(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,null,cv);
                lines++;
            }
            db.setTransactionSuccessful(); // Commit
            APlayerApplication.getContext().getContentResolver().notifyChange(uri,null);
        } finally {
            db.endTransaction(); //结束事务
        }
        return lines;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = DBManager.getInstance().openDataBase();
        int match = mUriMatcher.match(uri);
        Uri newUri = Uri.EMPTY;
        try {
            if(match == PLAY_LIST_MULTIPLE || match == PLAY_LIST_SONG_MULTIPLE){
                long rowId = db.insert(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,null,values);
                LogUtil.d("DBTest","rowId:" + rowId);
                if(rowId > 0){
                    newUri = ContentUris.withAppendedId(match == PLAY_LIST_MULTIPLE ? PlayLists.CONTENT_URI : PlayListSongs.CONTENT_URI,rowId);
                    APlayerApplication.getContext().getContentResolver().notifyChange(newUri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = DBManager.getInstance().openDataBase();
        int match = mUriMatcher.match(uri);
        int deleteRow = 0;
        try {
            deleteRow = db.delete(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME, selection, selectionArgs);
            APlayerApplication.getContext().getContentResolver().notifyChange(uri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
        }catch (Exception e){
            e.printStackTrace();
        }

        return deleteRow;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = DBManager.getInstance().openDataBase();
        int match = mUriMatcher.match(uri);
        int updateRow = 0;
        try {
            updateRow = db.delete(match == PLAY_LIST_MULTIPLE ? PlayLists.TABLE_NAME : PlayListSongs.TABLE_NAME,
                    selection,selectionArgs);
            APlayerApplication.getContext().getContentResolver().notifyChange(uri,null/**match == PLAY_LIST_MULTIPLE ? mPlayListObserver : mPlayListSongObserver*/);
        } catch (Exception e){
            e.printStackTrace();
        }

        return updateRow;
    }


    public static final String PLAY_LIST_CONTENT_TYPE = "vnd.android.cursor.dir/play_list";
    public static final String PLAY_LIST_ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/play_list";

    public static final String PLAY_LIST_SONG_CONTENT_TYPE = "vnd.android.cursor.dir/play_list_song";
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
