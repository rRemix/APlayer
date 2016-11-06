package remix.myplayer.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.model.Genre;
import remix.myplayer.model.MP3Item;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 数据库工具类
 *
 */
public class MediaStoreUtil {
    private static final String TAG = "MediaStoreUtil";
    private static Context mContext;
    private MediaStoreUtil(){}
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 获得所有歌曲id
     * @return
     */
    public static ArrayList<Integer> getAllSongsId() {
        ArrayList<Integer> mAllSongList = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;

        //默认过滤文件大小500K
        Constants.SCAN_SIZE = SPUtil.getValue(mContext,"Setting","ScanSize",-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 512000;
            SPUtil.putValue(mContext,"Setting","ScanSize",512000);
        }

        try{
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA},
                    Constants.MEDIASTORE_WHERE_SIZE, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if(cursor != null) {
                Global.mFolderMap.clear();
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    mAllSongList.add(id);
                    //根据歌曲路径对歌曲按文件夹分类
                    String full_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    SortWithFolder(id,full_path);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return mAllSongList;
    }

    /**
     * 将歌曲按文件夹分类
     * @param id 歌曲id
     * @param fullpath 歌曲完整路径
     */
    public static void SortWithFolder(int id,String fullpath) {
        String dirpath = fullpath.substring(0, fullpath.lastIndexOf("/"));
        if (!Global.mFolderMap.containsKey(dirpath)) {
            ArrayList<Integer> list = new ArrayList<>();
            list.add(id);
            Global.mFolderMap.put(dirpath, list);
        } else {
            ArrayList<Integer> list = Global.mFolderMap.get(dirpath);
            list.add(id);
        }
    }


    /**
     * 根据歌手或者专辑id获取所有歌曲
     * @param id 歌手id 专辑id
     * @param type 1:专辑  2:歌手
     * @return 对应所有歌曲的id
     */
    public static ArrayList<MP3Item> getMP3InfoByArg(int id, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<MP3Item> mp3Infolist = new ArrayList<>();
        try {
            if (type == Constants.ALBUM) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.ALBUM_ID + "=" + id + " and " + Constants.MEDIASTORE_WHERE_SIZE, null, null);
            }
            if (type == Constants.ARTIST) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.ARTIST_ID + "=" + id + " and " + Constants.MEDIASTORE_WHERE_SIZE, null, null);
            }

            if(cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    mp3Infolist.add(getMP3Info(cursor));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return mp3Infolist;
    }

    /**
     * 查找设置的专辑、艺术家、播放列表封面
     * @param id
     * @param type
     * @return
     */
    public static File getImageUrlInCache(int id,int type){
        //如果是专辑或者艺术家，先查找本地缓存
        File img = type == Constants.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + CommonUtil.hashKeyForDisk(id * 255 + ""))
                : type == Constants.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + CommonUtil.hashKeyForDisk(id * 255 + ""))
                : new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + CommonUtil.hashKeyForDisk(id * 255 + ""));
        return img;
    }

    /**
     * 根据参数和类型获得专辑封面
     * @param arg 参数,包括歌曲id、歌曲名、专辑id、播放列表id
     * @param type 查询类型
     * @return 专辑url
     */
    public static String getImageUrl(String arg,int type) {
        if(arg == null || arg.equals(""))
            return null;
        //如果是专辑或者艺术家，先查找本地缓存
        if(type == Constants.URL_ARTIST || type == Constants.URL_ALBUM || type == Constants.URL_PLAYLIST ){
            File img = type == Constants.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + CommonUtil.hashKeyForDisk(Integer.valueOf(arg) * 255 + ""))
                    : type == Constants.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + CommonUtil.hashKeyForDisk(Integer.valueOf(arg) * 255 + ""))
                    : new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + CommonUtil.hashKeyForDisk(Integer.valueOf(arg) * 255 + ""));
            if(img.exists()){
                return img.getAbsolutePath();
            }
            //没有设置过封面，对于播放列表类型的查找播放列表下所有歌曲，直到有一首歌曲存在封面
            if(type == Constants.URL_PLAYLIST){
                ArrayList<Integer> songIdList = PlayListUtil.getIDList(Integer.valueOf(arg));
                for (Integer songId : songIdList){
                    MP3Item item = MediaStoreUtil.getMP3InfoById(songId);
                    if(item == null)
                        continue;
                    String imgUrl = getAlbumUrlByAlbumId(item.getAlbumId());
                    if(imgUrl != null && !imgUrl.equals("")) {
                        File playlistImgFile = new File(imgUrl);
                        if(playlistImgFile.exists()) {
                            return playlistImgFile.getAbsolutePath();
                        }
                    }
                }
                return "";
            }
        }

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        String selection = null;
        String[] selectionArg = null;

        switch (type) {
            case Constants.URL_ARTIST:
                selection = MediaStore.Audio.Media.ARTIST_ID + "=?";
                selectionArg = new String[]{arg + ""};
                break;
            case Constants.URL_ALBUM:
                selection = MediaStore.Audio.Albums._ID + "=?";
                selectionArg = new String[]{arg + ""};
                break;
        }
        String album_art = "";
        try {
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                    selection,selectionArg,null);
            if(cursor != null && cursor.moveToFirst()) {
                album_art = cursor.getString(0);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return album_art;

    }

    /**
     * 根据专辑id查询图片url
     * @param albumid 专辑id
     * @return 专辑图片路径
     */
    public static String getAlbumUrlByAlbumId(int albumid) {
        Cursor cursor = null;
        String url = "";
        try {
            ContentResolver resolver = mContext.getContentResolver();
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.AlbumColumns.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + "=" + albumid,
                    null, null);
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                url = cursor.getString(0);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
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
     * @param albumId 专辑id
     * @param isthumb 是否是缩略图
     * @return 专辑图片的bitmap
     */
    public static Bitmap getAlbumBitmap(int albumId, boolean isthumb) {
        ParcelFileDescriptor pfd = null;
        try {
            Bitmap bm = null;
            File imgFile = getImageUrlInCache(albumId,Constants.URL_ALBUM);
            if(imgFile.exists()){
                bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            } else {
                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId);
                pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
//            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), id);
            if(bm == null)
                return null;
            Bitmap thumb = null;
            if(isthumb && bm.getWidth() > 150 && bm.getHeight() > 150)
                thumb = Bitmap.createScaledBitmap(bm, 150, 150, true);
            else
                thumb = Bitmap.createScaledBitmap(bm, 350, 350, true);
            if(bm != null && !bm.isRecycled()) {
                bm = null;
            }
            return thumb;
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
    public static MP3Item getMP3Info(Cursor cursor) {
        if(cursor == null || cursor.getColumnCount() <= 0)
            return null;

        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        return new MP3Item(
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                CommonUtil.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),CommonUtil.SONGTYPE),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                CommonUtil.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),CommonUtil.ALBUMTYPE),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                CommonUtil.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),CommonUtil.ARTISTTYPE),
                duration,
                CommonUtil.getTime(duration),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)),
                null,
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)));
    }


    /**
     * 根据多个歌曲id返回多个歌曲详细信息
     * @param idList 歌曲id列表
     * @return 对应所有歌曲信息列表
     */
    public static ArrayList<MP3Item> getMP3ListByIds(ArrayList<Integer> idList) {
        if(idList == null)
            return new ArrayList<>();
        String[] arg = new String[idList.size()];
        String where = "";
        for(int i = 0 ; i < idList.size() ;i++){
            arg[i] = idList.get(i) + "";
            where += (MediaStore.Audio.Media._ID + "=?");
            if(i != idList.size() - 1){
                where += " or ";
            }
            if(i == idList.size() - 1)
                where += (" and " + Constants.MEDIASTORE_WHERE_SIZE);
        }

        Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,where,arg,null,null);
        ArrayList<MP3Item> list = new ArrayList<>();
        if(cursor != null ){
            while (cursor.moveToNext()){
                list.add(getMP3Info(cursor));
            }
            //如果本地删除了某些歌曲 添加一些空的歌曲信息，保证点击播放列表前后歌曲数目一致
            if(cursor.getCount() < idList.size()){
                for(int i = cursor.getCount(); i < idList.size() ;i++){
                    MP3Item item = new MP3Item();
                    item.Title = mContext.getString(R.string.song_lose_effect);
                    item.Id = idList.get(i);
                    list.add(item);
                }
            }
        }
        return list;

    }

    /**
     * 根据歌曲id查询歌曲详细信息
     * @param id  歌曲id
     * @return 对应歌曲信息
     */
    public static MP3Item getMP3InfoById(int id) {
        MP3Item mp3Item = null;
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media._ID + "=" + id +
                            " and " + Constants.MEDIASTORE_WHERE_SIZE, null, null);
            if(cursor == null || cursor.getCount() == 0)
                return null;
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                cursor.moveToFirst();
            }
            mp3Item = getMP3Info(cursor);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return mp3Item;
    }


    /**
     * 建立genreId与audioId的映射
     * @param audioid
     * @param genreId
     */
    public static boolean insertGenreMap(int audioid, int genreId){
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Genres.Members.AUDIO_ID,audioid);
            Uri uri = mContext.getContentResolver().insert(MediaStore.Audio.Genres.Members.getContentUri("external",genreId),cv);
            return uri != null && ContentUris.parseId(uri) > 0;

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 插入一条新的流派
     */
    public static long insertGenre(String genre){
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Genres.NAME,genre);
            Uri uri = mContext.getContentResolver().insert(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,cv);
            return uri != null ? ContentUris.parseId(uri) : -1;
        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 根据歌曲id获得流派信息
     * @param audioId
     * @return
     */
    public static Genre getGenre(int audioId){
        Cursor genreCursor = null;
        Genre genre = new Genre();
        try {
            genreCursor = mContext.getContentResolver().query(MediaStore.Audio.Genres.getContentUriForAudioId("external",audioId),
                    null,null,null,null);
            if(genreCursor != null && genreCursor.getCount() > 0 && genreCursor.moveToFirst()){
                genre.GenreID = genreCursor.getInt(genreCursor.getColumnIndex(MediaStore.Audio.Genres._ID));
                genre.GenreName = genreCursor.getString(genreCursor.getColumnIndex(MediaStore.Audio.Genres.NAME));
            }

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(genreCursor != null && !genreCursor.isClosed())
                genreCursor.close();
        }
        return genre;
    }

    /**
     * 更新流派
     * @param genreId
     * @param genreName
     * @return
     */
    public static int updateGenre(int genreId,String genreName){
        int updateRow = 0;
        try {
            ContentValues genreCv = new ContentValues();
            genreCv.put(MediaStore.Audio.Genres.NAME,genreName);
            updateRow =  mContext.getContentResolver().update(
                    MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    genreCv,
                    MediaStore.Audio.Genres._ID + "=" + genreId,null) ;
        } catch (Exception e){
            e.printStackTrace();
        }
        return updateRow;
    }

    /**
     * 更新歌曲信息
     * @param id
     * @param title
     * @param artist
     * @param album
     * @param year
     * @return
     */
    public static int updateMP3Info(int id,String title,String artist,String album,String year){
        int updateRow = 0;
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Media.TITLE,title);
            cv.put(MediaStore.Audio.Media.ARTIST,artist);
            cv.put(MediaStore.Audio.Media.ALBUM,album);
            cv.put(MediaStore.Audio.Media.YEAR,year);
            updateRow = mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cv,
                    MediaStore.Audio.Media._ID + "=" + id,null);
        } catch (Exception e){
            e.printStackTrace();
        }
        return updateRow;
    }

    /**
     * 删除歌曲
     * @param data 删除参数 包括歌曲路径、专辑id、艺术家id、播放列表id、文件夹索引
     * @param type 删除类型 包括单个歌曲、专辑、艺术家、、文件夹、播放列表
     * @return 是否删除成功
     */
    public static boolean delete(int data, int type) {
        ContentResolver resolver = mContext.getContentResolver();
        String where = "";
        String[] arg = null;
        String folderName =  "";
        //删除文件夹时歌曲id列表
        ArrayList<Integer> idList = new ArrayList<>();

        int deleteNumInMediaStore = 0;
        int deleteNumInSD = 0;

        //拼接参数
        switch (type) {
            case Constants.SONG:
                where = MediaStore.Audio.Media._ID + "=?";
                arg = new String[]{data + ""};
                break;
            case Constants.ALBUM:
                where = new String(MediaStore.Audio.Media.ALBUM_ID + "=?");
                arg = new String[]{data + ""};
                break;
            case Constants.ARTIST:
                where = new String(MediaStore.Audio.Media.ARTIST_ID + "=?");
                arg = new String[]{data + ""};
                break;
            case Constants.FOLDER:
                folderName = CommonUtil.getMapkeyByPosition(Global.mFolderMap,data);
                idList = Global.mFolderMap.get(folderName);
                arg = new String[idList.size()];
                for(int i = 0 ; i < idList.size() ;i++){
                    arg[i] = idList.get(i) + "";
                    where += (MediaStore.Audio.Media._ID + "=?");
                    if(i != idList.size() - 1){
                        where += " or ";
                    }
                }
                break;
        }
        Cursor cursor = null;

        //第一步先删除本地存储中
        if(type != Constants.FOLDER) {
            //如果是非文件夹
            //先查询出每首歌的路径再删除
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media._ID},
                        where, arg, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        idList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        if(CommonUtil.deleteFile(path))
                            deleteNumInSD++;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(cursor != null && !cursor.isClosed())
                    cursor.close();
            }

        } else {
            //如果是删除文件夹
            //根据文件夹名字获得歌曲列表
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA},
                        where, arg, null);
                if(cursor != null && cursor.getCount() > 0){
                    while (cursor.moveToNext()){
                        String path = cursor.getString(0);
                        if(CommonUtil.deleteFile(path))
                            deleteNumInSD++;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(cursor != null && !cursor.isClosed())
                    cursor.close();
            }

        }

        //第二步删除mediastore中记录
        if(type != Constants.FOLDER) {
            //如果是非文件夹直接删除
            deleteNumInMediaStore = resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, arg);
        }else {
            //如果是文件夹
            //根据文件夹名获得对应所有歌曲列表,再根据每首歌曲id来删除
            deleteNumInMediaStore += resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,where,arg);
//            for (Integer id : idList) {
//                if(Global.mPlayQueue.contains(id)){
//                    Global.mPlayQueue.remove(id);
//                }
//                deleteNumInMediaStore += resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where,
//                        new String[]{id + ""});
//            }
        }
        //删除正在播放列表中的歌曲
//        boolean deleteSuccess = deleteNumInMediaStore > 0 && deleteNumInSD > 0;
//        if(deleteSuccess){
//            ArrayList<Integer> deleteIdList = new ArrayList<>();
//            for(Integer deleteId : idList){
//                if(Global.mPlayQueue.contains(deleteId)){
//                    deleteIdList.add(deleteId);
//                }
//            }
//            return Global.mPlayQueue.removeAll(deleteIdList);
//        } else {
//            return false;
//        }
        return deleteNumInMediaStore > 0 && deleteNumInSD > 0;
    }

    /**
     * 根据参数获得id列表
     * @param arg 专辑id 艺术家id 文件夹position 播放列表id
     * @param type
     * @return
     */
    public static ArrayList<Integer> getSongIdList(Object arg , int type){
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<Integer> ids = new ArrayList<>();
        //专辑或者艺术家
        if(type == Constants.ALBUM || type == Constants.ARTIST){
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Media._ID},
                        (type == Constants.ALBUM ? MediaStore.Audio.Media.ALBUM_ID : MediaStore.Audio.Media.ARTIST_ID) + "=" + arg +
                                " and " + Constants.MEDIASTORE_WHERE_SIZE,
                        null, null);
                if(cursor != null){
                    while (cursor.moveToNext()){
                        ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                    }
                }

            } catch (Exception e){
                e.printStackTrace();
            } finally {
                if(cursor != null && !cursor.isClosed()){
                    cursor.close();
                }
            }
        }

        //文件夹
        if(type == Constants.FOLDER){
            Iterator it = Global.mFolderMap.keySet().iterator();
            String path = "";
            for(int i = 0 ; i <= (int)arg ; i++)
                path = it.next().toString();
            ids = Global.mFolderMap.get(path);
        }
        //播放列表
        if(type == Constants.PLAYLIST){
            ids = PlayListUtil.getIDList((Integer) arg);
        }

        return ids;
    }


    /**
     * 获得所有歌曲id
     * @param cursor
     * @return
     */
    public static ArrayList<Integer> getSongIdListByCursor(Cursor cursor){
        ArrayList<Integer> ids = new ArrayList<>();
        if(cursor != null && !cursor.isClosed() && cursor.moveToFirst()){
            while (cursor.moveToNext()){
                ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            }
        }
        return ids;
    }


}
