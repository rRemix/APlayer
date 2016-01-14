package remix.myplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Created by Remix on 2015/11/30.
 */
public class Utility {
    private final static String TAG = "Utility";
    public static Utility mInstace = null;
    public static ArrayList<Long> mAllSongList = new ArrayList<>();
    public static ArrayList<Long> mPlayList = new ArrayList<>();
    public static Map<String, ArrayList<MP3Info>> mFolderList = new HashMap<>();
    public static Map<String,LinkedList<Long>> mFolderMap = new HashMap<>();
    private static Context mContext;

    public Utility(Context ctx) {
        mInstace = this;
        this.mContext = ctx;
    }

    //返回所有歌曲id，作为打开时默认的播放列表
    public static ArrayList<Long> getAllSongsId() {
        ArrayList<Long> mAllSongList = new ArrayList<>();
        //查询sd卡上所有音乐文件信息，过滤小于800k的
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.SIZE + ">80000", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            if (id > 0)
                mAllSongList.add(Long.valueOf(id));
            String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            SortWithFolder(id,url);
        }

        cursor.close();
        if (mAllSongList.size() > 0)
            return mAllSongList;
        return null;
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

    public static Bitmap getBitmapByArtistId(int id)
    {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        if(id < 0 ) return null;
        Bitmap bitmap = null;
        cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.ARTIST_ID + "=" + id, null, null);
        if(cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int songid = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                bitmap = CheckBitmapBySongId(songid,true);
                if(bitmap != null)
                    break;
            }
        }
        cursor.close();
        return bitmap;
    }

    /*
    type 0:专辑 1:歌手
     */
    //根据歌手或者专辑id获取所有歌曲
    public static ArrayList<MP3Info> getMP3InfoByArtistIdOrAlbumId(int _id, int type) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<MP3Info> mp3Info = new ArrayList<>();
        if (type == 0) {
            cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media.ALBUM_ID + "=" + _id, null, null);
            }

        if (type == 1) {
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
                String realtime = getTime(duration);
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
    public static String CheckUrlBySongId(long songId)
    {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"},
                MediaStore.Audio.Albums._ID + "=" + songId, null, null);
        if(cursor != null && cursor.getCount() > 0)
        {
            cursor.moveToNext();
            String album_url = "";
            album_url = cursor.getString(0);
            cursor.close();
            if (!album_url.equals(""))
                return album_url;
        }
        return null;
    }

    //根据专辑id查询图片url
    public static String CheckUrlByAlbumId(long Id)
    {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"},
                MediaStore.Audio.Albums._ID + "=" + Id,
                null, null);
        if(cursor != null && cursor.getCount() > 0)
        {
            cursor.moveToNext();
            String album_url = "";
            album_url = cursor.getString(0);
            cursor.close();
            if (album_url != null && !album_url.equals(""))
                return album_url;
        }
        return null;
    }
    //根据专辑id查询图片
    public static Bitmap CheckBitmapByAlbumId(int albumId,boolean isthumb)
    {
        try {
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
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
                    Log.i(TAG,bm.toString());
//                    bm.recycle();
//                    bm = null;
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
                    bm.recycle();
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
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            name = name.substring(0, name.lastIndexOf("."));
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            String realtime = getTime(duration);
            String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            mp3info = new MP3Info(id, name, album, albumId,artist, duration, realtime, url, size, null);
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

    //高斯模糊
    public static Bitmap doBlur(Bitmap sentBitmap, int radius,
                                boolean canReuseInBitmap)
    {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }
        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }

    //获得歌曲时长，分：秒
    public static String getTime(long duration)
    {
        int minute = (int)duration / 1000 / 60;
        int second = ((int)duration - minute * 60000) / 1000;
        //如果分钟数小于10
        if(minute < 10)
        {
            if(second < 10)
                return "0" + minute + ":0" + second;
            else
                return "0" + minute + ":" + second;
        }
        else
        {
            if(second < 10)
                return minute + ":0" + second;
            else
                return minute + ":" + second;
        }
    }
    //判断网路是否连接
    public static boolean isNetWorkConnected()
    {
        if(mContext != null)
        {
            ConnectivityManager mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetWorkInfo = mConnectivityManager.getActiveNetworkInfo();
            if(mNetWorkInfo != null)
                return mNetWorkInfo.isAvailable() && mNetWorkInfo.isConnected();
        }
        return false;
    }

    public final static String CTL_ACTION = "remix.music.CTL_ACTION";
    public final static String UPDATE_ACTION = "remix.music.UPDATE_ACTION";
    public final static String UPDATE_SEEKBAR = "remix.music.UPDATE_SEEKBAR";
    //控制命令
    public final static int PLAYSELECTEDSONG = 0;
    public final static int PREV = 1;
    public final static int PLAY = 2;
    public final static int NEXT = 3;
    public final static int PAUSE = 4;
    public final static int CONTINUE = 5;
    public final static int PLAY_LOOP = 6;
    public final static int PLAY_SHUFFLE = 7;
    //当前状态
    public final static int STATUS_PLAY = 0x010;
    public final static int STATUS_PAUSE = 0x011;
    //更新seekbar、已播放时间、未播放时间的消息
    public final static int UPDATE_TIME = 0x100;
    //更新播放信息
    public final static int UPDATE_INFORMATION = 0x101;
    //更新背景
    public final static int UPDATE_BG = 0x102;
    //启动哪一个子fragment
    public final static int ALBUM_HOLDER = 1;
    public final static int ARTIST_HOLDER = 2;
    public final static int FOLDER_HOLDER = 3;

    //腾讯Api Id
    public final static String TECENT_APIID = "1105030910";
    //微博Api Id
    public final static String WEIBO_APIID = "949172486";
    //微信APi Id
    public final static String WECHAT_APIID = "wx10775467a6664fbb";

}
