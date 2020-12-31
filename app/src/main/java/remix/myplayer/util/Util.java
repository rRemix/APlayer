package remix.myplayer.util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.floatpermission.rom.RomUtils;
import timber.log.Timber;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 通用工具类
 */
public class Util {

  /**
   * 注册本地Receiver
   */
  public static void registerLocalReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    LocalBroadcastManager.getInstance(App.getContext()).registerReceiver(receiver, filter);
  }

  /**
   * 注销本地Receiver
   */
  public static void unregisterLocalReceiver(BroadcastReceiver receiver) {
    LocalBroadcastManager.getInstance(App.getContext()).unregisterReceiver(receiver);
  }

  public static void sendLocalBroadcast(Intent intent) {
    LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(intent);
  }

  public static void sendCMDLocalBroadcast(int cmd) {
    LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(MusicUtil.makeCmdIntent(cmd));
  }

  /**
   * 注销Receiver
   */
  public static void unregisterReceiver(Context context, BroadcastReceiver receiver) {
    try {
      if (context != null) {
        context.unregisterReceiver(receiver);
      }
    } catch (Exception e) {
    }
  }


  /**
   * 判断app是否运行在前台
   */
  public static boolean isAppOnForeground() {
    try {
      ActivityManager activityManager = (ActivityManager) App.getContext()
          .getSystemService(Context.ACTIVITY_SERVICE);
      String packageName = App.getContext().getPackageName();

      List<ActivityManager.RunningAppProcessInfo> appProcesses = null;
      if (activityManager != null) {
        appProcesses = activityManager.getRunningAppProcesses();
      }
      if (appProcesses == null) {
        return false;
      }

      for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
        if (appProcess.processName.equals(packageName) &&
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
          return true;
        }
      }
    } catch (Exception e) {
      Timber.w("isAppOnForeground(), ex: %s", e.getMessage());
      return App.getContext().isAppForeground();
    }
    return false;
  }


  /**
   * 震动
   */
  public static void vibrate(final Context context, final long milliseconds) {
    if (context == null) {
      return;
    }
    try {
      Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
      vibrator.vibrate(milliseconds);
    } catch (Exception ignore) {

    }

  }

  /**
   * 获得目录大小
   */
  public static long getFolderSize(File file) {
    long size = 0;
    try {
      File[] fileList = file.listFiles();
      for (int i = 0; i < fileList.length; i++) {
        // 如果下面还有文件
        if (fileList[i].isDirectory()) {
          size = size + getFolderSize(fileList[i]);
        } else {
          size = size + fileList[i].length();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return size;
  }

  /**
   * 删除某个目录
   */
  public static void deleteFilesByDirectory(File directory) {
    if (directory == null) {
      return;
    }
    if (directory.isFile()) {
      deleteFileSafely(directory);
      return;
    }
    if (directory.isDirectory()) {
      File[] childFile = directory.listFiles();
      if (childFile == null || childFile.length == 0) {
        deleteFileSafely(directory);
        return;
      }
      for (File f : childFile) {
        deleteFilesByDirectory(f);
      }
      deleteFileSafely(directory);
    }
  }

  /**
   * 安全删除文件 小米、华为等手机极有可能在删除一个文件后再创建同名文件出现bug
   */
  public static boolean deleteFileSafely(File file) {
    if (file != null) {
      String tmpPath = file.getParent() + File.separator + System.currentTimeMillis();
      File tmp = new File(tmpPath);
      return file.renameTo(tmp) && tmp.delete();
    }
    return false;
  }

  /**
   * 防止修改字体大小
   */
  public static void setFontSize(App Application) {
    Resources resource = Application.getResources();
    Configuration c = resource.getConfiguration();
    c.fontScale = 1.0f;
    resource.updateConfiguration(c, resource.getDisplayMetrics());
  }

  /**
   * 获得歌曲格式
   */
  public static String getType(String mimeType) {
    if (mimeType.equals(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
      return "mp3";
    } else if (mimeType.equals(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
      return "flac";
    } else if (mimeType.equals(MediaFormat.MIMETYPE_AUDIO_AAC)) {
      return "aac";
    } else if (mimeType.contains("ape")) {
      return "ape";
    } else {
      try {
        if (mimeType.contains("audio/")) {
          return mimeType.substring(6, mimeType.length() - 1);
        } else {
          return mimeType;
        }
      } catch (Exception e) {
        return mimeType;
      }
    }
  }

  /**
   * 转换时间
   *
   * @return 00:00格式的时间
   */
  public static String getTime(long duration) {
    int minute = (int) duration / 1000 / 60;
    int second = (int) (duration / 1000) % 60;
    //如果分钟数小于10
    if (minute < 10) {
      if (second < 10) {
        return "0" + minute + ":0" + second;
      } else {
        return "0" + minute + ":" + second;
      }
    } else {
      if (second < 10) {
        return minute + ":0" + second;
      } else {
        return minute + ":" + second;
      }
    }
  }

  /**
   * 检测 响应某个意图的Activity 是否存在
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY);
    return list != null && list.size() > 0;
  }

  /**
   * 安全的启动activity
   */
  public static void startActivitySafely(Context context, Intent intent) {
    if (isIntentAvailable(context, intent)) {
      context.startActivity(intent);
    }
  }

  /**
   * 判断网路是否连接
   */
  public static boolean isNetWorkConnected() {
    ConnectivityManager connectivityManager = (ConnectivityManager) App.getContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivityManager != null) {
      NetworkInfo netWorkInfo = connectivityManager.getActiveNetworkInfo();
      if (netWorkInfo != null) {
        return netWorkInfo.isAvailable() && netWorkInfo.isConnected();
      }
    }
    return false;
  }

  /**
   * 删除歌曲
   *
   * @param path 歌曲路径
   * @return 是否删除成功
   */
  public static boolean deleteFile(String path) {
    File file = new File(path);
    return file.exists() && file.delete();
  }

  /**
   * 处理歌曲名、歌手名或者专辑名
   *
   * @param origin 原始数据
   * @param type 处理类型 0:歌曲名 1:歌手名 2:专辑名 3:文件名
   * @return
   */
  public static final int TYPE_SONG = 0;
  public static final int TYPE_ARTIST = 1;
  public static final int TYPE_ALBUM = 2;
  public static final int TYPE_DISPLAYNAME = 3;

  public static String processInfo(String origin, int type) {
    if (type == TYPE_SONG) {
      if (origin == null || origin.equals("")) {
        return App.getContext().getString(R.string.unknown_song);
      } else {
//                return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
        return origin;
      }
    } else if (type == TYPE_DISPLAYNAME) {
      if (origin == null || origin.equals("")) {
        return App.getContext().getString(R.string.unknown_song);
      } else {
        return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
      }
    } else {
      if (origin == null || origin.equals("")) {
        return App.getContext()
            .getString(type == TYPE_ARTIST ? R.string.unknown_artist : R.string.unknown_album);
      } else {
        return origin;
      }
    }
  }

//    /**
//     * @param map
//     * @param position
//     * @return
//     */
//    public static <T extends Object> String getMapkeyByPosition(Map<String, List<T>> map, int position) {
//        if (map == null || map.size() == 0 || position < 0)
//            return "";
//        Iterator it = map.keySet().iterator();
//        String key = "";
//        for (int i = 0; i <= position; i++)
//            key = it.next().toString();
//        return key;
//    }


  /**
   * 判断是否连续点击
   *
   * @return
   */
  private static long mLastClickTime;
  private static final int INTERVAL = 500;

  public static boolean isFastDoubleClick() {
    long time = System.currentTimeMillis();
    long timeInterval = time - mLastClickTime;
    if (0 < timeInterval && timeInterval < INTERVAL) {
      return true;
    }
    mLastClickTime = time;
    return false;
  }

  /**
   * 返回关键词的MD值
   */
  public static String hashKeyForDisk(String key) {
    String cacheKey;
    try {
      final MessageDigest mDigest = MessageDigest.getInstance("MD5");
      mDigest.update(key.getBytes());
      cacheKey = bytesToHexString(mDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      cacheKey = String.valueOf(key.hashCode());
    }
    return cacheKey;
  }

  public static String bytesToHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      String hex = Integer.toHexString(0xFF & bytes[i]);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString();
  }

  /**
   * 浏览器打开指定地址
   */
  public static void openUrl(String url) {
    if (TextUtils.isEmpty(url)) {
      return;
    }
    Uri uri = Uri.parse(url);
    Intent it = new Intent(Intent.ACTION_VIEW, uri);
    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    App.getContext().startActivity(it);
  }

  /**
   * 根据字符串形式的时间，得到毫秒值
   *
   * @param strTime 时间字符串
   */
  public static int getMill(String strTime) {
    int min;
    int sec;
    int mill;
    if (strTime.substring(1, 3).matches("[0-9]*")) {
      min = Integer.parseInt(strTime.substring(1, 3));
    } else {
      return -1;
    }
    if (strTime.substring(4, 6).matches("[0-9]*")) {
      sec = Integer.parseInt(strTime.substring(4, 6));
    } else {
      return -1;
    }
    if (strTime.substring(7, 9).matches("[0-9]*")) {
      mill = Integer.parseInt(strTime.substring(7, 9));
    } else {
      return -1;
    }
    return min * 60000 + sec * 1000 + mill;
  }

  /**
   * 判断是否有权限
   */
  public static boolean hasPermissions(String[] permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
      for (String permission : permissions) {
        if (App.getContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 是否有读写权限
   */
  private static final String[] PERMISSION_STORAGE = new String[]{
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

  public static boolean hasStoragePermissions() {
    return hasPermissions(PERMISSION_STORAGE);
  }


  /**
   * 判断wifi是否打开
   */
  public static boolean isWifi(Context context) {
    NetworkInfo activeNetInfo = ((ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    return activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI;
  }

  /**
   * 获取app当前的渠道号或application中指定的meta-data
   *
   * @return 如果没有获取成功(没有对应值 ， 或者异常)，则返回值为空
   */
  public static String getAppMetaData(String key) {
    if (TextUtils.isEmpty(key)) {
      return null;
    }
    String channelNumber = null;
    try {
      PackageManager packageManager = App.getContext().getPackageManager();
      if (packageManager != null) {
        ApplicationInfo applicationInfo = packageManager
            .getApplicationInfo(App.getContext().getPackageName(), PackageManager.GET_META_DATA);
        if (applicationInfo != null) {
          if (applicationInfo.metaData != null) {
            channelNumber = applicationInfo.metaData.getString(key);
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return channelNumber;
  }

  @NonNull
  public static Intent createShareSongFileIntent(@NonNull final Song song, Context context) {
    try {
      Parcelable parcelable = FileProvider.getUriForFile(context,
          context.getPackageName() + ".fileprovider",
          new File(song.getUrl()));
      return new Intent()
          .setAction(Intent.ACTION_SEND)
          .putExtra(Intent.EXTRA_STREAM,
              parcelable)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          .setType("audio/*");
    } catch (IllegalArgumentException e) {
      //the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
      e.printStackTrace();
      Toast.makeText(context, context.getString(R.string.cant_share_song), Toast.LENGTH_SHORT)
          .show();
      return new Intent();
    }
  }

  @NonNull
  public static Intent createShareImageFileIntent(@NonNull final File file, Context context) {
    try {
      Parcelable parcelable = FileProvider.getUriForFile(context,
          context.getPackageName() + ".fileprovider",
          file);
      return new Intent()
          .setAction(Intent.ACTION_SEND)
          .putExtra(Intent.EXTRA_STREAM,
              parcelable)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          .setType("image/*");
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      Toast.makeText(context, context.getString(R.string.cant_share_song), Toast.LENGTH_SHORT)
          .show();
      return new Intent();
    }
  }

  public static void closeSafely(Closeable closeable) {
    if (closeable != null) {
      if (closeable instanceof Cursor && ((Cursor) closeable).isClosed()) {
        return;
      }
      try {
        closeable.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  public static void installApk(Context context, String path) {
    if (path == null) {
      ToastUtil.show(context, context.getString(R.string.empty_path_report_to_developer));
      return;
    }
    File installFile = new File(path);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Uri apkUri = FileProvider.getUriForFile(context,
          context.getApplicationContext().getPackageName() + ".fileprovider", installFile);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
      context.startActivity(intent);
    } else {
      intent.setDataAndType(Uri.fromFile(installFile), "application/vnd.android.package-archive");
      context.startActivity(intent);
    }
  }

  /**
   * 获取进程号对应的进程名
   *
   * @param pid 进程号
   * @return 进程名
   */
  public static String getProcessName(int pid) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
      String processName = reader.readLine();
      if (!TextUtils.isEmpty(processName)) {
        processName = processName.trim();
      }
      return processName;
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    return null;
  }

  /**
   * 判断是否支持状态栏歌词
   */
  public static boolean isSupportStatusBarLyric(Context context) {
    return RomUtils.checkIsMeizuRom() || Settings.System.getInt(context.getContentResolver(), "status_bar_show_lyric", 0) != 0;
  }
}
