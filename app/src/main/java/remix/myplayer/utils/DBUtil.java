package remix.myplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 数据库工具类
 *
 */
public class DBUtil {
    private static final String TAG = "DBUtil";
    /**
     * 所有歌曲id
     */
    public static ArrayList<Long> mAllSongList = new ArrayList<>();

    /**
     * 正在播放歌曲id
     */
    public static ArrayList<Long> mPlayingList = new ArrayList<>();


    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String,ArrayList<Long>> mFolderMap = new HashMap<>();


    public static ArrayList<Long> mTodayList = new ArrayList<>();
    public static ArrayList<Long> mWeekList = new ArrayList<>();


    private static Context mContext;
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 更新内存与本地的正在播放列表
     * @param list
     */
    public static void setPlayingList(ArrayList<Long> list) {
        mPlayingList = list;
        XmlUtil.updatePlayingList();
    }

    /**
     * 获得所有歌曲id
     * @return
     */
    public static ArrayList<Long> getAllSongsId() {


        //获得今天日期
        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        long today_mill = today.getTimeInMillis();
        long day_mill = (1000 * 3600 * 24);

        ArrayList<Long> mAllSongList = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;


        //默认过滤文件大小500K
        Constants.SCAN_SIZE = SharedPrefsUtil.getValue(mContext,"setting","scansize",-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 512000;
            SharedPrefsUtil.putValue(mContext,"setting","scansize",512000);
        }

        try{
//            new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.ARTIST},
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            if(cursor != null) {

                DBUtil.mFolderMap.clear();
                while (cursor.moveToNext()) {
                    //计算歌曲添加时间
                    //如果满足条件添加到最近添加
                    long temp = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)) * 1000 ;
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date(temp));
                    int between = (int)((today_mill - calendar.getTimeInMillis()) / day_mill);
                    if(between <= 3 && between >= 0){
                        mWeekList.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                        if(between == 0){
                            mTodayList.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                        }
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    mAllSongList.add(id);

                    //根据歌曲路径对歌曲按文件夹分类
                    String full_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    SortWithFolder(id,full_path);
                }
                cursor.close();

            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null)
                cursor.close();
        }

        return mAllSongList;
    }

    /**
     * 根据文件夹名字获得所有歌曲id
     * @param foldername 文件夹名
     * @param position 索引
     * @return 该文件夹对应的所有歌曲id
     */
    public static ArrayList<Long> getIdsByFolderName(String foldername,int position){
        Iterator it = DBUtil.mFolderMap.keySet().iterator();
        String full_path = null;
        for(int i = 0 ; i <= position ; i++)
            full_path = it.next().toString();
        return DBUtil.mFolderMap.get(full_path);
    }


    /**
     * 将歌曲按文件夹分类
     * @param id 歌曲id
     * @param fullpath 歌曲完整路径
     */
    public static void SortWithFolder(long id,String fullpath) {
        String dirpath = fullpath.substring(0, fullpath.lastIndexOf("/"));
        if (!mFolderMap.containsKey(dirpath)) {
            ArrayList<Long> list = new ArrayList<>();
            list.add(id);
            mFolderMap.put(dirpath, list);
        } else {
            ArrayList<Long> list = mFolderMap.get(dirpath);
            list.add(id);
        }
    }


    /**
     * 根据歌手或者专辑id获取所有歌曲
     * @param _id 歌手或者专辑id
     * @param type 0:专辑 1:歌手
     * @return 对应所有歌曲的id
     */
    public static ArrayList<MP3Info> getMP3InfoByArtistIdOrAlbumId(int _id, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<MP3Info> mp3Infolist = new ArrayList<>();
        try {
            if (type == Constants.ALBUM_HOLDER) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.ALBUM_ID + "=" + _id + " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE, null, null);
            }

            if (type == Constants.ARTIST_HOLDER) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.ARTIST_ID + "=" + _id + " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE, null, null);
            }

            if(cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    mp3Infolist.add(getMP3Info(cursor));
                }
                cursor.close();
                return mp3Infolist;
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null)
                cursor.close();
        }

        return null;
    }


    /**
     * 根据参数和类型获得专辑封面
     * @param arg 参数,包括歌曲id、歌手id、歌曲名、专辑id
     * @param type 查询类型
     * @return 专辑url
     */
    public static String getImageUrl(String arg,int type) {
        if(arg == null || arg.equals(""))
            return null;

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        String selection = null;
        String[] selectionArg = null;

        switch (type) {
            case Constants.URL_ARTIST:
                selection = MediaStore.Audio.Media.ARTIST_ID + "=" + arg;
                selectionArg = null;
                break;
            case Constants.URL_SONGID:
                selection = MediaStore.Audio.Media._ID + "=" + arg;
                selectionArg = null;
                break;
            case Constants.URL_NAME:
                selection = MediaStore.Audio.Media.TITLE + "=?";
                selectionArg = new String[]{arg};
                break;
            case Constants.URL_ALBUM:
                selection = MediaStore.Audio.Albums._ID + "=" + arg;
                selectionArg = null;
        }
        try {
            String album_art = null;
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                    selection,selectionArg,null);
            if(cursor != null && cursor.moveToFirst()) {
                album_art = cursor.getString(0);
                cursor.close();
            }
            return album_art;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if(cursor != null)
                cursor.close();
        }
        return null;

    }

    /**
     * 根据专辑id查询图片url
     * @param Id 专辑id
     * @return 专辑url
     */
    public static String CheckUrlByAlbumId(long Id) {
        ContentResolver resolver = mContext.getContentResolver();
        String url = null;
        Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"},
                MediaStore.Audio.Albums._ID + "=" + Id,
                null, null);
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            url = cursor.getString(0);
            cursor.close();
        }

        return url;
    }

    //根据专辑id查询图片
//    public static Bitmap CheckBitmapByAlbumId(int albumId, boolean isthumb) {
//        try {
//            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
//            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
//            if (pfd != null) {
//                FileDescriptor fd = pfd.getFileDescriptor();
//                Bitmap bm = BitmapFactory.decodeFileDescriptor(fd);
//                if(bm == null)
//                    return null;
//                Bitmap thumb;
//                if(isthumb)
//                    thumb = Bitmap.createScaledBitmap(bm, 150, 150, true);
//                else
//                    thumb = Bitmap.createScaledBitmap(bm, 350, 350, true);
//                return thumb;
//            }
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return null;
//    }

    //

    /**
     * 根据歌曲id查询图片
     * @param id 歌曲id
     * @param isthumb 是否是缩略图
     * @return 专辑图片的bitmap
     */
    public static Bitmap CheckBitmapBySongId(int id,boolean isthumb) {
        ParcelFileDescriptor pfd = null;
        try {
            Uri uri = Uri.parse("content://media/external/audio/media/" + id + "/albumart");
            pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                Bitmap bm = BitmapFactory.decodeFileDescriptor(fd);
                if(bm == null)
                    return null;
                Bitmap thumb = null;
                if(isthumb)
                    thumb = Bitmap.createScaledBitmap(bm, 150, 150, true);
                else
                    thumb = Bitmap.createScaledBitmap(bm, 350, 350, true);
                if(bm != null && !bm.isRecycled()) {
                    bm = null;
                }

                return thumb;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(pfd != null)
                try {
                    pfd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }


    /**
     * 根据记录集获得歌曲详细
     * @param cursor 记录集
     * @return 拼装后的歌曲信息
     */
    public static MP3Info getMP3Info(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        name = name.substring(0, name.lastIndexOf("."));
        if(name.indexOf("unknown") > 0)
            name = "未知歌曲";

        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        if(artist.indexOf("unknown") > 0)
            artist = "未知歌手";

        String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        if(album.indexOf("unknown") > 0)
            album = "未知专辑";

        long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        String realtime = CommonUtil.getTime(duration);
        String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
        return new MP3Info(id, name, album, albumId,artist, duration, realtime, url, size, null);
    }


    /**
     * 根据多个歌曲id返回多个歌曲详细信息
     * @param ids 歌曲id列表
     * @return 对应所有歌曲信息列表
     */
    public static ArrayList<MP3Info> getMP3ListByIds(ArrayList<Long> ids) {
        ArrayList<MP3Info> list = new ArrayList<>();
        for (Long id : ids) {
            list.add(getMP3InfoById(id));
        }
        return list.size() > 0 ? list : null;
    }


    /**
     * 根据歌曲id查询歌曲详细信息
     * @param id  歌曲id
     * @return 对应歌曲信息
     */
    public static MP3Info getMP3InfoById(long id) {
        MP3Info mp3info = null;
        //查询sd卡上所有音乐文件信息
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media._ID + "=" + id +
                            " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE, null, null);
            if(cursor != null){
                cursor.moveToFirst();
                mp3info = getMP3Info(cursor);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null)
                cursor.close();
        }

        return mp3info;
    }



    /**
     * 删除歌曲
     * @param data 删除参数 包括歌曲路径、专辑id、艺术家id、文件夹名字
     * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹
     * @return 是否删除成功
     */
    public static boolean deleteSong(String data,int type) {
        ContentResolver resolver = mContext.getContentResolver();
        String where = null;
        String[] arg = null;
        //删除文件夹时歌曲列表
        ArrayList<Long> list = DBUtil.mFolderMap.get(data);
        int ret = 0;
        boolean ret2 = false;

        //拼接参数
        switch (type) {
            case Constants.DELETE_SINGLE:
                where = new String(MediaStore.MediaColumns.DATA + "=?");
                arg = new String[]{data};
                break;
            case Constants.DELETE_ALBUM:
                where = new String(MediaStore.Audio.Media.ALBUM_ID + "=?");
                arg = new String[]{data};
                break;
            case Constants.DELETE_ARTIST:
                where = new String(MediaStore.Audio.Media.ARTIST_ID + "=?");
                arg = new String[]{data};
                break;
            case Constants.DELETE_FOLDER:
                where = MediaStore.Audio.Media._ID + "=?";
                break;
        }
        Cursor cursor = null;

        //第一步先删除存储中
        if(type != Constants.DELETE_FOLDER) {
            //如果是非文件夹
            //先根据查询出每首歌的路径再删除
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media._ID},
                        where, arg, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        ret2 = CommonUtil.deleteFile(path);
                        //删除正在播放列表
                        mPlayingList.remove(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                        //删除播放列表中该歌曲
                        deleteSongInPlayList(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));

                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(cursor != null)
                    cursor.close();
            }

        } else {
            //如果是删除文件夹
            //直接根据文件夹名字获得歌曲列表
            try {
                for(int i = 0 ; i < list.size() ;i++){
                    cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA},
                            where, new String[]{String.valueOf(list.get(i))}, null);
                    if(cursor != null && cursor.moveToFirst()){
                        String path = cursor.getString(0);
                        ret2 = CommonUtil.deleteFile(path);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(cursor != null)
                    cursor.close();
            }

        }

        //第二步删除mediastore中记录
        if(type != Constants.DELETE_FOLDER) {
            //如果是非文件夹直接删除
            ret = resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, arg);
        }else {
            //如果是文件夹
            //根据文件夹名获得对应所有歌曲列表,再根据每首歌曲id来删除
            list = DBUtil.mFolderMap.get(data);
            if (list == null)
                return false;
            where = MediaStore.Audio.Media._ID + "=?";
            for (Long id : list) {
                ret += resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where,
                        new String[]{String.valueOf(id)});
                //删除正在播放列表中该歌曲
                mPlayingList.remove(id);
                //删除播放列表中该歌曲
                deleteSongInPlayList(id);
            }
        }
        if (ret > 0) {
            //删除全部歌曲列表中该歌曲
            mAllSongList = getAllSongsId();
            XmlUtil.updatePlayingList();
            XmlUtil.updatePlaylist();
//            mPlayingList = XmlUtil.getPlayingList();
//            DBUtil.mFolderMap.remove(data);
            if (FolderAdapter.mInstance != null)
                FolderAdapter.mInstance.notifyDataSetChanged();
        }
        if(cursor != null)
            cursor.close();
        return ret > 0 && ret2;
    }


    /**
     * 根据歌曲id查找所有播放列表中是否有该歌曲
     * 如果存在删除
     * @param id 需要删除的歌曲id
     */
    public static void deleteSongInPlayList(long id){
        Iterator it = PlayListActivity.getPlayList().keySet().iterator();
        ArrayList<PlayListItem> list = new ArrayList<>();

        while (it.hasNext()){
            list = PlayListActivity.getPlayList().get(it.next());
            if(list != null){
                for(PlayListItem item : list){
                    if(item.getId() == id) {
                        list.remove(item);
                        break;
                    }
                }
            }
        }
    }


}
