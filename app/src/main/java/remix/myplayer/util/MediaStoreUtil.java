package remix.myplayer.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.bean.mp3.Genre;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.ImageUriRequest;

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
//    public static String BASE_SELECTION = " and is_music = 1 ";
    public static String BASE_SELECTION = " ";
    public static void setContext(Context context){
        mContext = context;
    }

    public static List<Artist> getAllArtist(){
        ArrayList<Artist> artists = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"distinct " + MediaStore.Audio.Media.ARTIST_ID,MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE +  MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ARTIST_ID,
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,SortOrder.ArtistSortOrder.ARTIST_A_Z));
            if(cursor != null){
                while (cursor.moveToNext()){
                    artists.add(new Artist(cursor.getInt(0),cursor.getString(1)));
                }
            }
        }finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return artists;
    }

    public static List<Album> getALlAlbum(){
        ArrayList<Album> albums = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"distinct " + MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST_ID,
                            MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ALBUM_ID,
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,SortOrder.AlbumSortOrder.ALBUM_A_Z));
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    albums.add(new Album(cursor.getInt(0),
                            Util.processInfo(cursor.getString(1), Util.ALBUMTYPE),
                            cursor.getInt(2),
                            Util.processInfo(cursor.getString(3), Util.ARTISTTYPE)));
                }
            }
        }finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return albums;
    }

    public static List<Song> getAllSong(){
        ArrayList<Song> songs = new ArrayList<>();
        Cursor cursor = null;

        //默认过滤文件大小1MB
        Constants.SCAN_SIZE = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SCAN_SIZE,-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 1024 * ByteConstants.KB;
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SCAN_SIZE,Constants.SCAN_SIZE);
        }
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + getBaseSelection(),
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    songs.add(getMP3Info(cursor));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return songs;
    }

    public static List<Song> getLastAddedSong(){
        Cursor cursor = null;
        List<Song> songs = new ArrayList<>();
        try {
            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " >= " + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)) +
                            " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                    null,
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    songs.add(MediaStoreUtil.getMP3Info(cursor));
                }
            }
        }finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return songs;
    }

    /**
     * 获得所有歌曲id
     * @return
     */
    public static ArrayList<Integer> getAllSongsId() {
        ArrayList<Integer> allSongList = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;

        //默认过滤文件大小500K
        Constants.SCAN_SIZE = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 500 * ByteConstants.KB;
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",500 * ByteConstants.KB);
        }
        try{
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    allSongList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return allSongList;
    }

    /**
     * 获得文件夹信息
     * @return
     */
    public static Map<String,List<Integer>> getFolder(){
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        Map<String,List<Integer>> folder = new TreeMap<>(String::compareToIgnoreCase);


        //默认过滤文件大小500K
        Constants.SCAN_SIZE = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 500 * ByteConstants.KB;
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",500 * ByteConstants.KB);
        }

        try{
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                    null,
                    null);
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    //根据歌曲路径对歌曲按文件夹分类
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    sortFolder(folder,id,path);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return folder;
    }

    /**
     * 获得所有歌曲id 并按文件夹分类
     * @return
     */
    public static ArrayList<Integer> getAllSongsIdWithFolder() {
        ArrayList<Integer> allSongList = new ArrayList<>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;

        //默认过滤文件大小500K
        Constants.SCAN_SIZE = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",-1);
        if( Constants.SCAN_SIZE < 0) {
            Constants.SCAN_SIZE = 500 * ByteConstants.KB;
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"ScanSize",500 * ByteConstants.KB);
        }

        try{
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"SortOrder",MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
                            + SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"AscDesc"," asc"));
            if(cursor != null) {
                Global.FolderMap.clear();
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    allSongList.add(id);
                    //根据歌曲路径对歌曲按文件夹分类
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    sortFolder(Global.FolderMap,id,path);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return allSongList;
    }

    /**
     * 将歌曲按文件夹分类
     * @param id 歌曲id
     * @param fullPath 歌曲完整路径
     */
    public static void sortFolder(Map<String,List<Integer>> folder, int id, String fullPath) {
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
     * @param id 歌手id 专辑id
     * @param type 1:专辑  2:歌手
     * @return 对应所有歌曲的id
     */
    public static ArrayList<Song> getMP3InfoByArg(int id, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<Song> songInfolist = new ArrayList<>();
        try {
            if (type == Constants.ALBUM) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                         MediaStore.Audio.Media.ALBUM_ID + "=" + id + " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                        null,
                        SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_SONG_SORT_ORDER,SortOrder.ChildHolderSongSortOrder.SONG_A_Z));
            }
            if (type == Constants.ARTIST) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.ARTIST_ID + "=" + id + " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                        null,
                        SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_SONG_SORT_ORDER,SortOrder.ChildHolderSongSortOrder.SONG_A_Z));
            }

            if(cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    songInfolist.add(getMP3Info(cursor));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return songInfolist;
    }

    /**
     * 查找设置的专辑、艺术家、播放列表封面
     * @param id
     * @param type
     * @return
     */
    public static File getImageUrlInCache(int id,int type){
        //如果是专辑或者艺术家，先查找本地缓存
        return type == ImageUriRequest.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + Util.hashKeyForDisk(id * 255 + "")) :
               type == ImageUriRequest.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + Util.hashKeyForDisk(id * 255 + "")) :
               new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + Util.hashKeyForDisk(id * 255 + ""));
    }

    /**
     * 设置专辑封面
     * @param simpleDraweeView
     * @param albumId
     */
    public static void setImageUrl(SimpleDraweeView simpleDraweeView,int albumId){
        //先判断是否设置过封面
        File imgFile = MediaStoreUtil.getImageUrlInCache(albumId, ImageUriRequest.URL_ALBUM);
        if(imgFile != null && imgFile.exists()) {
            simpleDraweeView.setImageURI(Uri.parse("file://" + imgFile));
        } else {
            simpleDraweeView.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId));
        }
    }

    /**
     * 根据参数和类型获得专辑封面
     * @param arg 参数,包括歌曲id、歌曲名、专辑id、播放列表id
     * @param type 查询类型
     * @return 专辑url
     */
    public static String getImageUrl(int arg,int type) {
        if(arg <= 0)
            return null;
        //先查找本地缓存
        if(type == ImageUriRequest.URL_ARTIST || type == ImageUriRequest.URL_ALBUM || type == ImageUriRequest.URL_PLAYLIST ){
            File img = type == ImageUriRequest.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + Util.hashKeyForDisk(arg * 255 + ""))
                    : type == ImageUriRequest.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + Util.hashKeyForDisk(arg* 255 + ""))
                    : new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + Util.hashKeyForDisk(arg * 255 + ""));
            if(img.exists()){
                return "file://" + img.getAbsolutePath();
            }
            //没有设置过封面，对于播放列表类型的查找播放列表下所有歌曲，直到有一首歌曲存在封面
            if(type == ImageUriRequest.URL_PLAYLIST){
                List<Integer> songIdList = PlayListUtil.getIDList(arg);
                for (Integer songId : songIdList){
                    Song item = MediaStoreUtil.getMP3InfoById(songId);
                    if(item == null)
                        continue;
                    String imgUrl = getAlbumUrlByAlbumId(item.getAlbumId());
                    if(!TextUtils.isEmpty(imgUrl)) {
                        File playlistImgFile = new File(imgUrl);
                        if(playlistImgFile.exists()) {
                            return "file://" + playlistImgFile.getAbsolutePath();
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
            case ImageUriRequest.URL_ARTIST:
                selection = MediaStore.Audio.Media.ARTIST_ID + "=?";
                selectionArg = new String[]{arg + ""};
                break;
            case ImageUriRequest.URL_ALBUM:
//                selection = MediaStore.Audio.Albums._ID + "=?";
//                selectionArg = new String[]{arg + ""};
                return "content://media/external/audio/albumart/" + arg;

        }
        String album_art = "";
        try {
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                    selection,selectionArg,null);
            if(cursor != null && cursor.moveToFirst()) {
                album_art = "file://" + cursor.getString(0);
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
            File imgFile = getImageUrlInCache(albumId, ImageUriRequest.URL_ALBUM);
            if(imgFile.exists()){
                bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            } else {
                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId);
                pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                if(pfd == null)
                    return null;
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
            if(bm == null)
                return null;
            Bitmap thumb = null;
            if(isthumb && bm.getWidth() > 150 && bm.getHeight() > 150)
                thumb = Bitmap.createScaledBitmap(bm, 150, 150, true);
            else
                thumb = Bitmap.createScaledBitmap(bm, 350, 350, true);
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
     * 根据歌曲id查询图片
     * @param albumId
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getAlbumBitmap(int albumId, int width,int height) {
        ParcelFileDescriptor pfd = null;
        FileDescriptor fd = null;
        try {
            File imgFile = getImageUrlInCache(albumId, ImageUriRequest.URL_ALBUM);
            BitmapFactory.Options options = new BitmapFactory.Options();
            if(imgFile.exists()){
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imgFile.getAbsolutePath(),options);
            } else {
                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId);
                pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                if(pfd == null || (fd = pfd.getFileDescriptor()) == null)
                    return null;
                BitmapFactory.decodeFileDescriptor(fd,new Rect(),options);
            }
            options.outWidth = Math.min(options.outWidth,width);
            options.outHeight = Math.min(options.outHeight,height);
            options.inJustDecodeBounds = false;
            return imgFile.exists() ? BitmapFactory.decodeFile(imgFile.getAbsolutePath(),options) : BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(),new Rect(),options);
        } catch (Exception e) {
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
    public static Song getMP3Info(Cursor cursor) {
        if(cursor == null || cursor.getColumnCount() <= 0)
            return null;

        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        return new Song(
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)), Util.SONGTYPE),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)), Util.ALBUMTYPE),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                Util.processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)), Util.ARTISTTYPE),
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                duration,
                Util.getTime(duration),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)));
    }

    /**
     * 根据文件夹名字
     * @return
     */
    public static ArrayList<Song> getMP3ListByFolderName(String folderName){
        Cursor cursor = null;
        ArrayList<Song> list = new ArrayList<>();
        try {
            List<Integer> ids = Global.FolderMap.get(folderName);
            StringBuilder selection = new StringBuilder(127);
            selection.append(MediaStore.Audio.Media._ID + " in (");
            for(int i = 0 ; i < ids.size();i++){
                selection.append(ids.get(i)).append( i == ids.size() - 1 ? ") " : ",");
            }
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    selection.toString(),
                    null,
                    SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_SONG_SORT_ORDER,SortOrder.ChildHolderSongSortOrder.SONG_A_Z));
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    list.add(getMP3Info(cursor));
                }
            }
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return list;
    }

    /**
     * 根据多个歌曲id返回多个歌曲详细信息
     * @param idList 歌曲id列表
     * @return 对应所有歌曲信息列表
     */
    public static ArrayList<Song> getMP3ListByIds(ArrayList<Integer> idList) {
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
                where += ( " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE +  MediaStoreUtil.getBaseSelection() );
        }

        Cursor cursor = null;
        ArrayList<Song> list = new ArrayList<>();
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,where,arg,null,null);
            if(cursor != null ){
                while (cursor.moveToNext()){
                    list.add(getMP3Info(cursor));
                }
                //如果本地删除了某些歌曲 添加一些空的歌曲信息，保证点击播放列表前后歌曲数目一致
                if(cursor.getCount() < idList.size()){
                    for(int i = cursor.getCount(); i < idList.size() ;i++){
                        Song item = new Song();
                        item.Title = mContext.getString(R.string.song_lose_effect);
                        item.Id = idList.get(i);
                        list.add(item);
                    }
                }
            }
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return list;

    }

    /**
     * 根据歌曲id查询歌曲详细信息
     * @param id  歌曲id
     * @return 对应歌曲信息
     */
    public static Song getMP3InfoById(int id) {
        Song song = null;
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media._ID + "=" + id +
                            " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(), null, null);
            if(cursor == null || cursor.getCount() == 0)
                return null;
            if(cursor.getCount() > 0 && cursor.moveToFirst()){
                cursor.moveToFirst();
            }
            song = getMP3Info(cursor);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return song;
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
     * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹、播放列表
     * @return 是否歌曲数量
     */
    public static int delete(int data, int type,boolean deleteSource){
        List<Song> songs = new ArrayList<>();
        String where = null;
        String[] arg = null;

        //拼接参数
        switch (type){
            case Constants.SONG:
                where = MediaStore.Audio.Media._ID + "=?";
                arg = new String[]{data + ""};
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
                if(type == Constants.ALBUM) {
                    where = MediaStore.Audio.Media.ALBUM_ID + "=?";
                    arg = new String[]{data + ""};
                } else {
                    where = MediaStore.Audio.Media.ARTIST_ID + "=?";
                    arg = new String[]{data + ""};
                }
                break;
            case Constants.FOLDER:
                String folderName = Util.getMapkeyByPosition(Global.FolderMap,data);
                List<Integer> ids = Global.FolderMap.get(folderName);
                StringBuilder selection = new StringBuilder(127);
//                for(int i = 0 ; i < ids.size();i++){
//                    selection.append(MediaStore.Audio.Media._ID).append(" = ").append(ids.get(i)).append(i != ids.size() - 1 ? " or " : " ");
//                }
                selection.append(MediaStore.Audio.Media._ID + " in (");
                for(int i = 0 ; i < ids.size();i++){
                    selection.append(ids.get(i)).append( i == ids.size() - 1 ? ") " : ",");
                }
                where = selection.toString();
                arg = null;
                break;
        }

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    where,arg,null);
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    songs.add(getMP3Info(cursor));
                }
            }
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return delete(songs,deleteSource);
    }

    /**
     * 删除指定歌曲
     * @param songs
     * @param deleteSource
     * @return
     */
    public static int delete(List<Song> songs,boolean deleteSource){
        if(songs == null || songs.size() == 0)
            return 0;

        //删除之前保存的所有移除歌曲id
        Set<String> deleteId = new HashSet<>(SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"DeleteID"));
        //保存到sp
        for(Song temp : songs){
            deleteId.add(temp.getId() + "");
        }
        SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "DeleteID", deleteId);
        mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null);
        //删除源文件
        if(deleteSource)
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
//        Set<String> deleteId = new HashSet<>(SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"DeleteID"));
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
//        SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "DeleteID", deleteId);
//        mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null);
//
//        //二.删除源文件
//        if(SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.DELETE_SOURCE,false))
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
     * @param songs
     */
    public static void deleteSource(List<Song> songs){
        if(songs == null || songs.size() == 0)
            return;
        for(Song song : songs){
            Util.deleteFileSafely(new File(song.getUrl()));
        }
    }

    /**
     * 根据参数获得id列表
     * @param arg 专辑id 艺术家id 文件夹position 播放列表id
     * @param type
     * @return
     */
    public static List<Integer> getSongIdList(Object arg , int type){
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        List<Integer> ids = new ArrayList<>();
        //专辑或者艺术家
        if(type == Constants.ALBUM || type == Constants.ARTIST){
            try {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Media._ID},
                        (type == Constants.ALBUM ? MediaStore.Audio.Media.ALBUM_ID : MediaStore.Audio.Media.ARTIST_ID) + "=" + arg +
                                " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
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
            Iterator it = Global.FolderMap.keySet().iterator();
            String path = "";
            for(int i = 0 ; i <= (int)arg ; i++)
                path = it.next().toString();
            ids = Global.FolderMap.get(path);
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
        if(cursor != null && !cursor.isClosed() && cursor.getCount() > 0){
            while (cursor.moveToNext()){
                ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            }
        }
        return ids;
    }

    /**
     * 过滤移出的歌曲以及铃声等
     * @return
     */
    public static String getBaseSelection(){
//        Set<String> deleteId = SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"DeleteID");
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

        Set<String> deleteId = SPUtil.getStringSet(mContext,SPUtil.SETTING_KEY.SETTING_NAME,"DeleteID");
        if(deleteId == null || deleteId.size() == 0)
            return BASE_SELECTION;
        StringBuilder blacklist = new StringBuilder();
        blacklist.append(" and ");
        int i = 0;
        for(String id : deleteId){
            if(i == 0){
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
     * @param url
     * @return
     */
    public static int getSongIdByUrl(String url){
        if(TextUtils.isEmpty(url))
            return -1;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    MediaStore.Audio.Media.DATA + "=?",
                    new String[]{url},
                    null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                return cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            }
        } finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * 设置铃声
     * @param audioId
     */
    public static void setRing(Context context,int audioId) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
            cv.put(MediaStore.Audio.Media.IS_ALARM, false);
            cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
            // 把需要设为铃声的歌曲更新铃声库
            if(mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) > 0) {
                Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
                ToastUtil.show(context,R.string.set_ringtone_success);
            }
            else
                ToastUtil.show(context,R.string.set_ringtone_error);
        }catch (Exception e){
            //没有权限
            if(e instanceof SecurityException){
                ToastUtil.show(context,R.string.please_give_write_settings_permission);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(mContext)) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                }
            }

        }

    }

}
