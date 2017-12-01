package remix.myplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import remix.myplayer.misc.cache.DiskCache;

/**
 * Created by Remix on 2017/11/30.
 */

public class ImageUriUtil {
    private ImageUriUtil(){}
    private static final String TAG = "ImageUriUtil";
    private static Context mContext;
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 获得某歌手在本地数据库的封面
     */
    public static File getArtistThumbInMediaCache(int artistId){
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Media.ARTIST_ID + "=?",new String[]{artistId + ""},null);
            if(cursor != null && cursor.moveToFirst()) {
                return new File(cursor.getString(0));
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return null;
    }

    /**
     * 判断某专辑在本地数据库是否有封面
     * @param uri
     * @return
     */
    public static boolean isAlbumThumbExistInMediaCache(Uri uri){
        boolean exist = false;
        InputStream stream = null;
        try {
            stream = mContext.getContentResolver().openInputStream(uri);
            exist = true;
        } catch (Exception e) {
            exist = false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return exist;
    }

    /**
     * 返回自定义的封面
     * @param arg
     * @param type
     * @return
     */
    public static File getCustomThumbIfExist(int arg, int type){
        File img = type == Constants.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + CommonUtil.hashKeyForDisk(arg * 255 + ""))
                : type == Constants.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + CommonUtil.hashKeyForDisk(arg* 255 + ""))
                : new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + CommonUtil.hashKeyForDisk(arg * 255 + ""));
        if(img.exists()){
            return img;
        }
        return null;
    }
}
