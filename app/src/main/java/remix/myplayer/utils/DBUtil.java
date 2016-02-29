package remix.myplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.infos.MP3Info;

/**
 * Created by taeja on 16-2-17.
 */
public class DBUtil {
    private static final String TAG = "DBUtil";
    public static ArrayList<Long> mAllSongList = new ArrayList<>();
    public static ArrayList<Long> mPlayingList = new ArrayList<>();
    public static ArrayList<String> mSearchKeyList = new ArrayList<>();
    public static Map<String,LinkedList<Long>> mFolderMap = new HashMap<>();
    public static ArrayList<String> mFolderList = new ArrayList<>();

    private static Context mContext;
    public static void setContext(Context context){
        mContext = context;
    }

    public static void setPlayingList(ArrayList<Long> list)
    {
        mPlayingList = list;
        XmlUtil.updatePlayingListXml();
    }
    //返回所有歌曲id
    public static ArrayList<Long> getAllSongsId() {
        ArrayList<Long> mAllSongList = new ArrayList<>();
        //查询sd卡上所有音乐文件信息，过滤小于800k的
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;

        try{
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.SIZE + ">80000", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            if(cursor != null) {
                DBUtil.mFolderMap.clear();
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    mAllSongList.add(id);
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    mSearchKeyList.add(album);
                    mSearchKeyList.add(artist);
                    mSearchKeyList.add(title);
                    String full_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    SortWithFolder(id,full_path);
                }
                cursor.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return mAllSongList.size() > 0 ? mAllSongList : null;
    }

    //根据文件夹名字获得所有歌曲id
    public static LinkedList<Long> getIdsByFolderName(String foldername,int position){
        Iterator it = DBUtil.mFolderMap.keySet().iterator();
        String full_path = null;
        for(int i = 0 ; i <= position ; i++)
            full_path = it.next().toString();
        return (LinkedList)DBUtil.mFolderMap.get(full_path);
    }

    //将歌曲按文件夹分类
    public static void SortWithFolder(long id,String fullpath) {
        String dirpath = fullpath.substring(0, fullpath.lastIndexOf("/"));
        if (!mFolderMap.containsKey(dirpath)) {
            LinkedList<Long> list = new LinkedList<>();
            list.add(id);
            mFolderMap.put(dirpath, list);
        } else {
            LinkedList<Long> list = mFolderMap.get(dirpath);
            list.add(id);
        }
    }
    //获得所有文件夹
    public static void SortWithFolder(String path) {
        if(!mFolderList.contains(path))
            mFolderList.add(path);
    }
    /*
    type 0:专辑 1:歌手
    flag 是否需要完整信息
     */
    //根据歌手或者专辑id获取所有歌曲
    public static ArrayList<MP3Info> getMP3InfoByArtistIdOrAlbumId(int _id, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<MP3Info> mp3Info = new ArrayList<>();
        if (type == Constants.ALBUM_HOLDER) {
            cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media.ALBUM_ID + "=" + _id, null, null);
        }

        if (type == Constants.ARTIST_HOLDER) {
            cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media.ARTIST_ID + "=" + _id, null, null);
        }

        if(cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                name = name.substring(0, name.lastIndexOf("."));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                long albumid = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                String realtime = CommonUtil.getTime(duration);
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                MP3Info mp3temp = new MP3Info(id, name, album, albumid,artist, duration, realtime, url, size, null);
                mp3Info.add(mp3temp);
            }
            cursor.close();
            return mp3Info;
        }
        cursor.close();
        return null;
    }
    //根据歌曲id查询图片url
//    public static String CheckUrlBySongId(long songId)
//    {
//        ContentResolver resolver = mContext.getContentResolver();
//        Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"},
//                MediaStore.Audio.Media._ID + "=" + songId, null, null);
//        if(cursor != null && cursor.getCount() > 0)
//        {
//            cursor.moveToNext();
//            String album_url = "";
//            album_url = cursor.getString(0);
//            cursor.close();
//            if (!album_url.equals(""))
//                return album_url;
//        }
//        return null;
//    }



    //根据参数获得图片url
    // 0:专辑id 1:歌手id 2:歌曲id 3:歌曲名

    public static String getImageUrl(String arg,int type)
    {
        if(arg == null || arg.equals(""))
            return null;

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        String selection = null;
        String[] selectionArg = null;

        switch (type)
        {
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

    //根据专辑id查询图片url
    public static String CheckUrlByAlbumId(long Id)
    {
        ContentResolver resolver = mContext.getContentResolver();
        String url = null;
        Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"},
                MediaStore.Audio.Albums._ID + "=" + Id,
                null, null);
        if(cursor != null && cursor.getCount() > 0)
        {
            cursor.moveToNext();
            url = cursor.getString(0);
            cursor.close();
        }

        return url;
    }
    //根据专辑id查询图片
    public static Bitmap CheckBitmapByAlbumId(int albumId, boolean isthumb)
    {
        try {
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                Bitmap bm = BitmapFactory.decodeFileDescriptor(fd);
                if(bm == null)
                    return null;
                Bitmap thumb;
                if(isthumb)
                    thumb = Bitmap.createScaledBitmap(bm, 150, 150, true);
                else
                    thumb = Bitmap.createScaledBitmap(bm, 350, 350, true);
                return thumb;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    //根据歌曲id查询图片
    public static Bitmap CheckBitmapBySongId(int id,boolean isthumb)
    {
        try {
            Uri uri = Uri.parse("content://media/external/audio/media/" + id + "/albumart");
            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
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
                if(bm != null && !bm.isRecycled())
                {
                    bm = null;
                }
                return thumb;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }


    //根据文件夹名字返回多个歌曲信息
    public static ArrayList<MP3Info> getMP3ListByFolder(String name)
    {
        ArrayList<MP3Info> list = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME + "=?",
                new String[]{name}, null);
        if(cursor == null || cursor.getColumnCount() <= 0)
            return null;
        while (cursor.moveToNext())
        {
            list.add(getMP3Info(cursor));
        }
        cursor.close();
        return list;
    }

    //根据多个歌曲名字返回多个歌曲详细信息
    public static ArrayList<MP3Info> getMP3ListByNames(ArrayList<String> list)
    {
        ArrayList<MP3Info> mlist = new ArrayList<>();
        Cursor cursor = null;
        try {
            for(int i = 0 ; i < list.size(); i++)
            {
                cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.TITLE + "=?",
                        new String[]{list.get(i)}, null);
                cursor.moveToFirst();
                if (cursor != null && cursor.getCount() > 0)
                {
                    mlist.add(getMP3Info(cursor));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if(cursor != null)
                cursor.close();
        }
        return  mlist;
    }

    //根据记录集获得歌曲详细
    public static MP3Info getMP3Info(Cursor cursor)
    {
        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        name = name.substring(0, name.lastIndexOf("."));
        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        String realtime = CommonUtil.getTime(duration);
        String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
        return new MP3Info(id, name, album, albumId,artist, duration, realtime, url, size, null);
    }

    //根据多个歌曲id返回多个歌曲详细信息
    public static ArrayList<MP3Info> getMP3ListByIds(LinkedList<Long> ids)
    {
        ArrayList<MP3Info> list = new ArrayList<>();
        for (Long id : ids)
        {
            list.add(getMP3InfoById(id));
        }
        return list.size() > 0 ? list : null;
    }
    //根据歌曲id查询歌曲详细信息
    public static MP3Info getMP3InfoById(long Baseid) {
        MP3Info mp3info = null;
        //查询sd卡上所有音乐文件信息，过滤小于800k的
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media._ID + "=" + Baseid, null, null);
        while (cursor.moveToNext()) {
            mp3info = getMP3Info(cursor);
        }
        cursor.close();
        if(mp3info != null)
            return mp3info;
        return null;
    }

    //根据歌手id获得歌手的歌曲数
    public static int getSongNumByArtistId(long ArtistId)
    {
        Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.ARTIST_ID + "=" + ArtistId, null, null);
        int count = cursor.getCount();
        cursor.close();
        if(count > 0)
            return count;
        return -1;
    }

    //删除某一首歌曲
    public static boolean deleteSong(String data,int type)
    {

        ContentResolver resolver = mContext.getContentResolver();
        String where = null;
        String[] arg = null;
        ArrayList<String> datas = new ArrayList<>();
        LinkedList<Long> list = DBUtil.mFolderMap.get(data);
        int ret = 0;
        boolean ret2 = false;

        //删除sd卡上文件
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
        if(type != Constants.DELETE_FOLDER) {
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA},
                        where, arg, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
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
        else {
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

        //删除mediastore中记录
        if(type != Constants.DELETE_FOLDER) {
            ret = resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, arg);
        }else {
            list = DBUtil.mFolderMap.get(data);
            if (list == null)
                return false;
            where = MediaStore.Audio.Media._ID + "=?";
            for (Long id : list) {
                ret += resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where,
                        new String[]{String.valueOf(id)});
            }
        }
        if (ret > 0) {
            //删除播放列表与全部歌曲列表中该歌曲
            mAllSongList = getAllSongsId();
            mPlayingList = XmlUtil.getPlayingList();
//            DBUtil.mFolderMap.remove(data);
            if (FolderAdapter.mInstance != null)
                FolderAdapter.mInstance.notifyDataSetChanged();
        }
        return ret > 0 && ret2;
    }

    //压缩图片用于分享
    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
