package remix.myplayer.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.common.util.ByteConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import remix.myplayer.App;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.SortOrder;

import static remix.myplayer.util.Util.hasStoragePermissions;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 数据库工具类
 */
public class MediaStoreUtil {
    private static final String TAG = "MediaStoreUtil";
    private static Context mContext;

    private MediaStoreUtil() {
    }

    //    public static String BASE_SELECTION = " and is_music = 1 ";
    private static String BASE_SELECTION = " ";

    public static void setContext(Context context) {
        mContext = context;
    }

    static {
        Constants.SCAN_SIZE = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE, ByteConstants.KB * 500);
    }

    public static List<Artist> getAllArtist() {
        if (!hasStoragePermissions())
            return new ArrayList<>();
        ArrayList<Artist> artists = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"distinct " + MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ARTIST},
                    MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ARTIST_ID,
                    null,
                    SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    artists.add(new Artist(cursor.getInt(0), cursor.getString(1),
                            0));
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return artists;
    }

    public static List<Album> getAllAlbum() {
        if (!hasStoragePermissions())
            return new ArrayList<>();
        ArrayList<Album> albums = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    null, null, null, null);

            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"distinct " + MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST_ID,
                            MediaStore.Audio.Media.ARTIST},
                    MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ALBUM_ID,
                    null,
                    SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    albums.add(new Album(cursor.getInt(0),
                            Util.processInfo(cursor.getString(1), Util.ALBUMTYPE),
                            cursor.getInt(2),
                            Util.processInfo(cursor.getString(3), Util.ARTISTTYPE),
                            0));
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return albums;
    }

    public static List<Song> getAllSong() {
        if (!hasStoragePermissions())
            return new ArrayList<>();
        ArrayList<Song> songs = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    getBaseSelection(),
                    null,
                    SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    songs.add(getSongInfo(cursor));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return songs;
    }

    public static List<Song> getLastAddedSong() {
        Cursor cursor = null;
        List<Song> songs = new ArrayList<>();
        try {
            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " >= " + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)) + " and " + MediaStoreUtil.getBaseSelection(),
                    null,
                    MediaStore.Audio.Media.DATE_ADDED);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    songs.add(MediaStoreUtil.getSongInfo(cursor));
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return songs;
    }

    /**
     * 获得所有歌曲id
     *
     * @return
     */
    public static ArrayList<Integer> getAllSongsId() {
        ArrayList<Integer> allSongList = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;

        try {
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStoreUtil.getBaseSelection(),
                    null,
                    SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    allSongList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return allSongList;
    }

    /**
     * 获得文件夹信息
     *
     * @return
     */
    public static Map<String, List<Integer>> getFolder() {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        Map<String, List<Integer>> folder = new TreeMap<>(String::compareToIgnoreCase);

        try {
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA},
                    MediaStoreUtil.getBaseSelection(),
                    null,
                    null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    //根据歌曲路径对歌曲按文件夹分类
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    sortFolder(folder, id, path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return folder;
    }


    /**
     * 将歌曲按文件夹分类
     *
     * @param id       歌曲id
     * @param fullPath 歌曲完整路径
     */
    public static void sortFolder(Map<String, List<Integer>> folder, int id, String fullPath) {
        String dirPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
        if (!folder.containsKey(dirPath)) {
            List<Integer> list = new ArrayList<>();
            list.add(id);
            folder.put(dirPath, list);
        } else {
            List<Integer> list = folder.get(dirPath);
            list.add(id);
        }
    }


    /**
     * 根据歌手或者专辑id获取所有歌曲
     *
     * @param id   歌手id 专辑id
     * @param type 1:专辑  2:歌手
     * @return 对应所有歌曲的id
     */
    public static List<Song> getMP3InfoByArtistIdOrAlbumId(int id, int type) {
        String selection = null;
        String sortOrder = null;
        String[] selectionValues = null;
        if (type == Constants.ALBUM) {
            selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
            selectionValues = new String[]{id + ""};
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        }
        if (type == Constants.ARTIST) {
            selection = MediaStore.Audio.Media.ARTIST_ID + "=?";
            selectionValues = new String[]{id + ""};
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        }
        return getSongs(selection, selectionValues, sortOrder);
    }

    /**
     * 根据记录集获得歌曲详细
     *
     * @param cursor 记录集
     * @return 拼装后的歌曲信息
     */
    public static Song getSongInfo(Cursor cursor) {
        if (cursor == null || cursor.getColumnCount() <= 0)
            return null;

        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        final String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        final int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        return new Song(
                id,
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)), Util.SONGTYPE),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)), Util.ALBUMTYPE),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)), Util.ARTISTTYPE),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                duration,
                Util.getTime(duration),
                data,
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)));
    }

    /**
     * 根据文件夹名字
     *
     * @return
     */
    public static List<Song> getMP3ListByFolderName(String folderName) {
        List<Integer> ids = Global.FolderMap.get(folderName);
        if (ids == null || ids.size() == 0)
            return new ArrayList<>();
        StringBuilder selection = new StringBuilder(127);
        selection.append(MediaStore.Audio.Media._ID + " in (");
        for (int i = 0; i < ids.size(); i++) {
            selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
        }
        return getSongs(selection.toString(), null, SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z));
    }

    /**
     * 根据多个歌曲id返回多个歌曲详细信息
     *
     * @param idList 歌曲id列表
     * @return 对应所有歌曲信息列表
     */
    @Deprecated
    public static ArrayList<Song> getMP3ListByIds(ArrayList<Integer> idList) {
        if (idList == null)
            return new ArrayList<>();
        String[] arg = new String[idList.size()];
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < idList.size(); i++) {
            arg[i] = idList.get(i) + "";
            where.append(MediaStore.Audio.Media._ID + "=?");
            if (i != idList.size() - 1) {
                where.append(" or ");
            }
            if (i == idList.size() - 1)
                where.append(MediaStoreUtil.getBaseSelection());
        }

        Cursor cursor = null;
        ArrayList<Song> list = new ArrayList<>();
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, where.toString(), arg, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(getSongInfo(cursor));
                }
                //如果本地删除了某些歌曲 添加一些空的歌曲信息，保证点击播放列表前后歌曲数目一致
                if (cursor.getCount() < idList.size()) {
                    for (int i = cursor.getCount(); i < idList.size(); i++) {
                        Song item = new Song();
                        item.Title = mContext.getString(R.string.song_lose_effect);
                        item.Id = idList.get(i);
                        list.add(item);
                    }
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return list;

    }

    /**
     * 根据专辑id查询歌曲详细信息
     *
     * @param albumId 歌曲id
     * @return 对应歌曲信息
     */
    public static Song getMP3InfoByAlbumId(int albumId) {
        return getSong(MediaStore.Audio.Media.ALBUM_ID + "=?", new String[]{albumId + ""});
    }

    /**
     * 根据歌曲id查询歌曲详细信息
     *
     * @param id 歌曲id
     * @return 对应歌曲信息
     */
    public static Song getMP3InfoById(int id) {
        return getSong(MediaStore.Audio.Media._ID + "=?", new String[]{id + ""});
    }

    public static void insertAlbumArt(@NonNull Context context, int albumId, String path) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

        ContentValues values = new ContentValues();
        values.put("album_id", albumId);
        values.put("_data", path);

        contentResolver.insert(artworkUri, values);
    }

    public static void deleteAlbumArt(@NonNull Context context, int albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri localUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(localUri, albumId), null, null);
    }

    /**
     * 删除歌曲
     *
     * @param data 删除参数 包括歌曲路径、专辑id、艺术家id、播放列表id、文件夹索引
     * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹、播放列表
     * @return 是否歌曲数量
     */
    public static int delete(int data, int type, boolean deleteSource) {
        List<Song> songs = new ArrayList<>();
        String where = null;
        String[] arg = null;

        //拼接参数
        switch (type) {
            case Constants.SONG:
                where = MediaStore.Audio.Media._ID + "=?";
                arg = new String[]{data + ""};
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
                if (type == Constants.ALBUM) {
                    where = MediaStore.Audio.Media.ALBUM_ID + "=?";
                    arg = new String[]{data + ""};
                } else {
                    where = MediaStore.Audio.Media.ARTIST_ID + "=?";
                    arg = new String[]{data + ""};
                }
                break;
            case Constants.FOLDER:
                String folderName = Util.getMapkeyByPosition(Global.FolderMap, data);
                List<Integer> ids = Global.FolderMap.get(folderName);
                StringBuilder selection = new StringBuilder(127);
//                for(int i = 0 ; i < ids.size();i++){
//                    selection.append(MediaStore.Audio.Media._ID).append(" = ").append(ids.get(i)).append(i != ids.size() - 1 ? " or " : " ");
//                }
                selection.append(MediaStore.Audio.Media._ID + " in (");
                for (int i = 0; i < ids.size(); i++) {
                    selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
                }
                where = selection.toString();
                arg = null;
                break;
        }

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    where, arg, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    songs.add(getSongInfo(cursor));
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return delete(songs, deleteSource);
    }

    /**
     * 删除指定歌曲
     *
     * @param songs
     * @param deleteSource
     * @return
     */
    public static int delete(List<Song> songs, boolean deleteSource) {
        //保存是否删除源文件
        SPUtil.putValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, deleteSource);
        if (songs == null || songs.size() == 0)
            return 0;

        //删除之前保存的所有移除歌曲id
        Set<String> deleteId = new HashSet<>(SPUtil.getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG));
        //保存到sp
        for (Song temp : songs) {
            deleteId.add(temp.getId() + "");
        }
        SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG, deleteId);
        mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);
        //删除源文件
        if (deleteSource)
            deleteSource(songs);

        return songs.size();
    }

//    /**
//     * 删除歌曲
//     * @param data 删除参数 包括歌曲路径、专辑id、艺术家id、播放列表id、文件夹索引
//     * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹、播放列表
//     * @return 是否删除成功
//     */
//    public static int delete(int data, int type) {
//        int deleteNum = 0;
//        //删除之前保存的所有移除歌曲id
//        Set<String> deleteId = new HashSet<>(SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.BLACKLIST_SONG));
//        //待删除的id
//        List<Integer> wangToDelete = new ArrayList<>();
//
//        //一.添加到黑名单
//        Cursor cursor = null;
//        //拼接参数
//        switch (type) {
//            case Constants.SONG:
//                deleteNum = 1;
//                wangToDelete.add(data);
//                break;
//            case Constants.ALBUM:
//            case Constants.ARTIST:
//                String where;
//                String[] arg;
//                if(type == Constants.ALBUM) {
//                    where = MediaStore.Audio.Media.ALBUM_ID + "=?";
//                    arg = new String[]{data + ""};
//                } else {
//                    where = MediaStore.Audio.Media.ARTIST_ID + "=?";
//                    arg = new String[]{data + ""};
//                }
//                try {
//                    cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media._ID},where,arg,null);
//                    if(cursor != null && cursor.getCount() > 0){
//                        while (cursor.moveToNext()){
//                            wangToDelete.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
//                        }
//                        deleteNum = cursor.getCount();
//                    }
//                } finally {
//                    if(cursor != null && !cursor.isClosed())
//                        cursor.close();
//                }
//                break;
//            case Constants.FOLDER:
//                try {
//                    String folderName = Util.getMapkeyByPosition(Global.FolderMap,data);
//                    if(Global.FolderMap.get(folderName) != null){
//                        wangToDelete.addAll(Global.FolderMap.get(folderName));
//                        deleteNum = Global.FolderMap.get(folderName).size();
//                    }
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
//                break;
//        }
//        //保存到sp
//        for(Integer temp : wangToDelete){
//            deleteId.add(temp + "");
//        }
//
//        SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG, deleteId);
//        mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null);
//
//        //二.删除源文件
//        if(SPUtil.getValue(mContext,SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE,false))
//        deleteSource(wangToDelete);
//        return deleteNum;
//
//    }

//    /**
//     * 删除源文件
//     * @param ids
//     */
//    public static void deleteSource(List<Integer> ids){
//        if(ids == null || ids.size() == 0)
//            return;
//        Cursor cursor = null;
//        try {
//            StringBuilder selection = new StringBuilder(127);
//            for(int i = 0 ; i < ids.size();i++){
//                selection.append(MediaStore.Audio.Media._ID).append("=").append(ids.get(i)).append(i != ids.size() - 1 ? " and " : " ");
//            }
//            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.DATA},selection.toString(),null,null);
//            if(cursor != null && cursor.getCount() > 0){
//                while (cursor.moveToNext()){
//                    Util.deleteFileSafely(new File(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))));
//                }
//            }
//        } finally {
//            if(cursor != null && !cursor.isClosed())
//                cursor.close();
//        }
//    }

    /**
     * 删除源文件
     *
     * @param songs
     */
    public static void deleteSource(List<Song> songs) {
        if (songs == null || songs.size() == 0)
            return;
        for (Song song : songs) {
            Util.deleteFileSafely(new File(song.getUrl()));
        }
    }

    /**
     * 根据参数获得id列表
     *
     * @param arg  专辑id 艺术家id 文件夹position 播放列表id
     * @param type
     * @return
     */
    public static List<Integer> getSongIdList(Object arg, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        List<Integer> ids = new ArrayList<>();
        //专辑或者艺术家
        if (type == Constants.ALBUM || type == Constants.ARTIST) {
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Media._ID},
                        (type == Constants.ALBUM ? MediaStore.Audio.Media.ALBUM_ID : MediaStore.Audio.Media.ARTIST_ID) + "=" + arg + " and " + MediaStoreUtil.getBaseSelection(),
                        null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        //文件夹
        if (type == Constants.FOLDER) {
            Iterator it = Global.FolderMap.keySet().iterator();
            String path = "";
            for (int i = 0; i <= (int) arg; i++)
                path = it.next().toString();
            ids = Global.FolderMap.get(path);
        }
        //播放列表
        if (type == Constants.PLAYLIST) {
            ids = PlayListUtil.getIDList((Integer) arg);
        }

        return ids;
    }


    /**
     * 过滤移出的歌曲以及铃声等
     *
     * @return
     */
    public static String getBaseSelection() {
//        Set<String> deleteId = SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.BLACKLIST_SONG);
//        if(deleteId == null || deleteId.size() == 0)
//            return BASE_SELECTION;
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(" and ");
//        int i = 0;
//        for (String id : deleteId) {
//            stringBuilder.append(MediaStore.Audio.Media._ID + " != ").append(id).append(i != deleteId.size() - 1 ?  " and " : " ");
//            i++;
//        }
//        return stringBuilder.toString();

        Set<String> deleteId = SPUtil.getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG);
        if (deleteId == null || deleteId.size() == 0)
            return MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE;
        StringBuilder blacklist = new StringBuilder();
        blacklist.append(MediaStore.Audio.Media.SIZE + ">").append(Constants.SCAN_SIZE);
        blacklist.append(" and ");
        int i = 0;
        for (String id : deleteId) {
            if (i == 0) {
                blacklist.append(MediaStore.Audio.Media._ID).append(" not in (");
            }
            blacklist.append(id);
            blacklist.append(i != deleteId.size() - 1 ? "," : ")");
            i++;
        }
        return blacklist.append(BASE_SELECTION).toString();
    }

    /**
     * 根据路径获得歌曲id
     *
     * @param url
     * @return
     */
    public static int getSongIdByUrl(String url) {
        if (TextUtils.isEmpty(url) || !hasStoragePermissions())
            return -1;
        return getSongId(MediaStore.Audio.Media.DATA + " = ?", new String[]{url});
    }

    /**
     * 设置铃声
     *
     * @param audioId
     */
    public static void setRing(Context context, int audioId) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
            cv.put(MediaStore.Audio.Media.IS_ALARM, false);
            cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
            // 把需要设为铃声的歌曲更新铃声库
            if (mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) > 0) {
                Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
                ToastUtil.show(context, R.string.set_ringtone_success);
            } else
                ToastUtil.show(context, R.string.set_ringtone_error);
        } catch (Exception e) {
            //没有权限
            if (e instanceof SecurityException) {
                ToastUtil.show(context, R.string.please_give_write_settings_permission);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(mContext)) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (Util.isIntentAvailable(mContext, intent)) {
                            mContext.startActivity(intent);
                        }
                    }
                }
            }

        }
    }

    @Nullable
    public static Cursor makeSongCursor(@Nullable String selection, final String[] selectionValues, final String sortOrder) {
        if (selection != null && !selection.trim().equals("")) {
            selection = getBaseSelection() + " AND " + selection;
        } else {
            selection = getBaseSelection();
        }
        try {
            return mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, selection, selectionValues, sortOrder);
        } catch (SecurityException e) {
            return null;
        }
    }

    public static int getSongId(@Nullable String selection, String[] selectionValues) {
        List<Song> songs = getSongs(selection, selectionValues, null);
        return songs != null && songs.size() > 0 ? songs.get(0).getId() : -1;
    }


    public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues) {
        return getSongIds(selection, selectionValues, null);
    }

    public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues, String sortOrder) {
        List<Integer> songs = new ArrayList<>();
        try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    songs.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                }
            }
        } catch (Exception ignore) {

        }
        return songs;
    }

    public static Song getSong(@Nullable String selection, String[] selectionValues) {
        List<Song> songs = getSongs(selection, selectionValues, null);
        return songs != null && songs.size() > 0 ? songs.get(0) : null;
    }

    public static List<Song> getSongs(@Nullable String selection, String[] selectionValues, final String sortOrder) {
        List<Song> songs = new ArrayList<>();

        try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    songs.add(getSongInfo(cursor));
                }
            }
        } catch (Exception ignore) {

        }
        return songs;
    }

    public static List<Song> getSongs(@Nullable String selection, String[] selectionValues) {
        return getSongs(selection, selectionValues, SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, null));
    }
}
