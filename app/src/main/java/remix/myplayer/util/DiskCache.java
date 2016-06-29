package remix.myplayer.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by Remix on 2016/6/14.
 */
public class DiskCache {
    private static DiskLruCache mLrcDiskCache;
    private static DiskLruCache mCoverDiskCache;
    private static Context mContext;

    public static void init(Context context){
        mContext = context;
        try {
            File lrc_cacheDir = getDiskCacheDir(mContext, "lrc");
            if (!lrc_cacheDir.exists())
                lrc_cacheDir.mkdir();

            mLrcDiskCache = DiskLruCache.open(lrc_cacheDir, getAppVersion(mContext), 1, 5 * 1024 * 1024);

            File thumb_cacheDir = getDiskCacheDir(mContext,"cover");
            if(!thumb_cacheDir.exists())
                thumb_cacheDir.mkdir();
            mCoverDiskCache = DiskLruCache.open(thumb_cacheDir,getAppVersion(mContext),1,10 * 1024 * 1024);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DiskLruCache getLrcDiskCache(){
        return mLrcDiskCache;
    }

    public static DiskLruCache getCoverDiskCache(){return mCoverDiskCache;}

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
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
