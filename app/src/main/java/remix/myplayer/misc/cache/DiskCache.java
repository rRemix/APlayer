package remix.myplayer.misc.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import remix.myplayer.R;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2016/6/14.
 */
public class DiskCache {
    private static DiskLruCache mLrcCache;
    private static DiskLruCache mAlbumCache;
    private static DiskLruCache mArtistCache;
    private static Context mContext;

    public static void init(Context context){
        mContext = context;
        try {
            File lrcCacheDir = getDiskCacheDir(mContext, "lrc");
            if (!lrcCacheDir.exists())
                lrcCacheDir.mkdir();
            mLrcCache = DiskLruCache.open(lrcCacheDir, getAppVersion(mContext), 1, 2 * 1024 * 1024);

            File thumbnailCacheDir = getDiskCacheDir(mContext,"thumbnail");
            if(!thumbnailCacheDir.exists()){
                if(!thumbnailCacheDir.mkdir()){
                    ToastUtil.show(mContext, R.string.create_dir_error);
                }
            }

//            File album_cacheDir = getDiskCacheDir(mContext,"thumbnail/album");
//            if(!album_cacheDir.exists())
//                album_cacheDir.mkdir();
//            mAlbumCache = DiskLruCache.open(album_cacheDir,getAppVersion(mContext),1,10 * 1024 * 1024);
//
//            File artist_cacheDir = getDiskCacheDir(mContext,"thumbnail/artist");
//            if(!artist_cacheDir.exists())
//                artist_cacheDir.mkdir();
//            mAlbumCache = DiskLruCache.open(album_cacheDir,getAppVersion(mContext),1,10 * 1024 * 1024);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DiskLruCache getLrcDiskCache(){
        return mLrcCache;
    }

    public static DiskLruCache getAlbumDiskCache(){return mAlbumCache;}

    public static DiskLruCache getArtistCache(){return mArtistCache;}

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File file = context.getExternalCacheDir();
            if(file != null)
                cachePath = file.getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

}
