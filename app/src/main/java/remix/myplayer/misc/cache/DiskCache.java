package remix.myplayer.misc.cache;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import java.io.File;

/**
 * Created by Remix on 2016/6/14.
 */
public class DiskCache {

  public static File getDiskCacheDir(Context context, String uniqueName) {
    String cachePath = "";
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
        || !Environment.isExternalStorageRemovable()) {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        File[] files = context.getExternalCacheDirs();
        for (File file : files) {
          if (file != null && file.exists() && file.isDirectory()) {
            cachePath = file.getAbsolutePath();
            break;
          }
        }
      } else {
        File file = context.getExternalCacheDir();
        if (file != null && file.exists() && file.isDirectory()) {
          cachePath = file.getAbsolutePath();
        }
      }
    }
    if (TextUtils.isEmpty(cachePath)) {
      cachePath = context.getCacheDir().getPath();
    }
    return new File(cachePath + File.separator + uniqueName);
  }

}
