package remix.myplayer.util;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.bean.bmob.Error;
import remix.myplayer.lyric.SearchLrc;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 通用工具类
 */
public class Util {
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    /**
     * 注销receiver
     */
    public static void unregisterReceiver(Context context, BroadcastReceiver receiver){
        try {
            if(context != null){
                context.unregisterReceiver(receiver);
                receiver = null;
            }
        } catch (Exception e){
            LogUtil.e("unregisterReceiver error",e.toString());
        }
    }

    /**
     * 判断列表是否为空
     */
    public static boolean isEmptyList(List list){
        return list == null || list.size() == 0;
    }

    /**
     * 判断app是否运行在前台
     * @param context
     * @return
     */
    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }


    /**
     * 震动
     * @param context
     * @param milliseconds
     */
    public static void vibrate(final Context context, final long milliseconds) {
        if(context == null)
            return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        vibrator.vibrate(milliseconds);
    }

    /**
     * 获得目录大小
     * @param file
     * @return
     * @throws Exception
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
     * @param directory
     */
    public static void deleteFilesByDirectory(File directory) {
        if(directory == null)
            return;
        if(directory.isFile()){
            deleteFileSafely(directory);
            return;
        }
        if(directory.isDirectory()){
            File[] childFile = directory.listFiles();
            if(childFile == null || childFile.length == 0){
                deleteFileSafely(directory);
                return;
            }
            for(File f : childFile){
                deleteFilesByDirectory(f);
            }
            deleteFileSafely(directory);
        }
    }

    /**
     * 安全删除文件 小米、华为等手机极有可能在删除一个文件后再创建同名文件出现bug
     * @param file
     * @return
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
    public static void setFontSize(APlayerApplication Application) {
        Resources resource = Application.getResources();
        Configuration c = resource.getConfiguration();
        c.fontScale = 1.0f;
        resource.updateConfiguration(c, resource.getDisplayMetrics());
    }

    /**
     * 获得歌曲格式
     * @param mimeType
     * @return
     */
    public static String getType(String mimeType){
        if(mimeType.equals(MediaFormat.MIMETYPE_AUDIO_MPEG)){
            return "mp3";
        }
        else if (mimeType.equals(MediaFormat.MIMETYPE_AUDIO_FLAC))
            return "flac";
        else if(mimeType.equals(MediaFormat.MIMETYPE_AUDIO_AAC))
            return "aac";
        else if(mimeType.contains("ape"))
            return "ape";
        else {
            try {
                if(mimeType.contains("audio/"))
                    return mimeType.substring(6,mimeType.length() - 1);
                else
                    return mimeType;
            }catch (Exception e){
                return mimeType;
            }
        }
    }

    /**
     * 转换时间
     * @param duration
     * @return 00:00格式的时间
     */
    public static String getTime(long duration) {
        int minute = (int)duration / 1000 / 60;
        int second = ((int)duration - minute * 60000) / 1000;
        //如果分钟数小于10
        if(minute < 10) {
            if(second < 10)
                return "0" + minute + ":0" + second;
            else
                return "0" + minute + ":" + second;
        } else {
            if(second < 10)
                return minute + ":0" + second;
            else
                return minute + ":" + second;
        }
    }

    /**
     * 检测 响应某个意图的Activity 是否存在
     * @param context
     * @param intent
     * @return
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list != null && list.size() > 0;
    }

    /**
     * 判断网路是否连接
     * @return
     */
    public static boolean isNetWorkConnected() {
        if(mContext != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetWorkInfo = mConnectivityManager.getActiveNetworkInfo();
            if(mNetWorkInfo != null)
                return mNetWorkInfo.isAvailable() && mNetWorkInfo.isConnected();
        }
        return false;
    }

    /**
     * 删除歌曲
     * @param path 歌曲路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String path){
        File file = new File(path);
        return file.exists() && file.delete();
    }

    /**
     * 处理歌曲名、歌手名或者专辑名
     * @param origin 原始数据
     * @param type 处理类型 0:歌曲名 1:歌手名 2:专辑名
     * @return
     */
    public static final int SONGTYPE = 0;
    public static final int ARTISTTYPE = 1;
    public static final int ALBUMTYPE = 2;
    public static String processInfo(String origin,int type){
        if(type == SONGTYPE){
            if(origin == null || origin.equals("") || origin.contains("unknown") ){
                return mContext.getString(R.string.unknown_song);
            } else {
//                return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
                return origin;
            }
        } else{
            if(origin == null || origin.equals("") || origin.contains("unknown") ){
                return mContext.getString(type == ARTISTTYPE ? R.string.unknown_artist : R.string.unknown_album);
            } else {
                return origin;
            }
        }
    }

    /**
     *
     * @param map
     * @param position
     * @return
     */
    public static  <T extends Object> String getMapkeyByPosition(Map<String,List<T>> map, int position){
        if(map == null || map.size() == 0 || position < 0)
            return "";
        Iterator it = map.keySet().iterator();
        String key = "";
        for(int i = 0 ; i <= position ; i++)
            key = it.next().toString();
        return key;
    }



    /**
     * 判断是否连续点击
     * @return
     */
    private static long mLastClickTime;
    private static final int INTERVAL = 500;
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeInterval = time - mLastClickTime;
        if(0 < timeInterval && timeInterval < INTERVAL)
            return true;
        mLastClickTime = time;
        return false;
    }

    /**
     * 返回关键词的MD值
     * @param key
     * @return
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
    public static void openUrl(String url){
        if(TextUtils.isEmpty(url))
            return;
        Uri uri = Uri.parse(url);
        Intent it = new Intent(Intent.ACTION_VIEW, uri);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(it);
    }

    /**
     * 根据歌曲名和歌手请求歌曲信息
     * @param songname
     * @param artistname
     * @return
     */
    public static JSONObject getSongJsonObject(String songname, String artistname,long duration){
        URL lrcIdUrl = null;
        try {
            //歌词迷
//            lrcIdUrl = new URL("http://gecimi.com/api/lyric/" + songname + "/" + artistname);
            //酷狗
            lrcIdUrl = new URL("http://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword="
                            + artistname + "-" + songname + "&duration=" + duration + "&hash=");
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        BufferedReader br = null;
        String s;
        StringBuilder strBuffer = new StringBuilder();
        try {
            if(lrcIdUrl == null)
                return new JSONObject("");
            HttpURLConnection httpConn = (HttpURLConnection) lrcIdUrl.openConnection();
            httpConn.setConnectTimeout(10000);
            httpConn.connect();
            InputStreamReader inReader = new InputStreamReader(httpConn.getInputStream());
            br = new BufferedReader(inReader);
            if(br == null)
                return null;
            while((s = br.readLine()) != null){
                strBuffer.append(s);
            }
            return new JSONObject(strBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * 查找歌曲的lrc文件
     * @param songName
     * @param searchPath
     */
    public static void searchFile(String displayName,String songName,String artistName,File searchPath) {
        //判断SD卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = searchPath.listFiles();
            if(files == null || files.length == 0)
                return;
            for(File file : files){
                if (file.isDirectory() && file.canRead()){
                    searchFile(displayName,songName,artistName,file);
                } else {
                    if(isRightLrc(file,displayName,songName,artistName)){
                        SearchLrc.CurrentLrcPath = file.getAbsolutePath();
                        return;
                    }
                }
            }
        }
    }

    /**
     * 判断是否是相匹配的歌词
     * @param file
     * @param title
     * @param artist
     * @return
     */
    public static boolean isRightLrc(File file,String displayName,String title,String artist){
        BufferedReader br = null;
        try {
            if(file == null || !file.canRead() || !file.isFile())
                return false;
            if(TextUtils.isEmpty(file.getAbsolutePath()) || TextUtils.isEmpty(displayName) ||
                    TextUtils.isEmpty(title) || TextUtils.isEmpty(artist))
                return false;
            //仅判断.lrc文件
            if(!file.getName().endsWith("lrc"))
                return false;
            //暂时忽略网易云的歌词
            if(file.getAbsolutePath().contains("netease/cloudmusic/"))
                return false;
            String fileName = file.getName().indexOf('.') > 0 ?
                    file.getName().substring(0,file.getName().lastIndexOf('.')) : file.getName();
            //判断歌词文件名与歌曲文件名是否一致
            if(fileName.toUpperCase().startsWith(displayName.toUpperCase())) {
                return true;
            }
            //判断是否包含歌手名和歌曲名
            if(fileName.toUpperCase().contains(title.toUpperCase()) && fileName.toUpperCase().contains(artist.toUpperCase())){
                return true;
            }
            //读取前五行歌词内容进行判断
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            boolean hasArtist = false;
            boolean hasTitle = false;
            for(int i = 0 ; i < 5;i++){
                String lrcLine = "";
                if((lrcLine = br.readLine()) == null)
                    break;
                if(lrcLine.contains("ar") && lrcLine.toUpperCase().contains(artist.toUpperCase())) {
                    hasArtist = true;
                    continue;
                }
                if(lrcLine.contains("ti") && lrcLine.toUpperCase().contains(title.toUpperCase())) {
                    hasTitle = true;
                }
            }
            if(hasArtist && hasTitle){
                return true;
            }
        } catch (Exception e) {

        } finally {
            try {
                if(br != null){
                    br.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 手动上传日志信息
     */
    public static void uploadException(String title,String description){
        try {
            if(!Util.isNetWorkConnected()){
                return;
            }
            PackageManager pm = APlayerApplication.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(APlayerApplication.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Error error = new Error(title,description,
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + "");
            error.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 手动上传异常
     */
    public static void uploadException(String title,Exception exception){
        try {
            if(!Util.isNetWorkConnected()){
                return;
            }
            PackageManager pm = APlayerApplication.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(APlayerApplication.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Error error = new Error(title,exception.toString(),
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + "");
            error.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据字符串形式的时间，得到毫秒值
     * @param strTime 时间字符串
     * @return
     */
    public static int getMill(String strTime) {
        int min;
        int sec;
        int mill;
        if(strTime.substring(1,3).matches("[0-9]*"))
            min = Integer.parseInt(strTime.substring(1, 3));
        else
            return -1;
        if(strTime.substring(4,6).matches("[0-9]*"))
            sec = Integer.parseInt(strTime.substring(4, 6));
        else
            return -1;
        if(strTime.substring(7,9).matches("[0-9]*"))
            mill = Integer.parseInt(strTime.substring(7,9));
        else
            return -1;
        return min * 60000 + sec * 1000 + mill;
    }

    /**
     * 判断是否有权限
     * @return
     */
    public static boolean hasPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (mContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断wifi是否打开
     * @return
     */
    public static boolean isWifi(Context context) {
        NetworkInfo activeNetInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 获取app当前的渠道号或application中指定的meta-data
     *
     * @return 如果没有获取成功(没有对应值，或者异常)，则返回值为空
     */
    public static String getAppMetaData(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        String channelNumber = null;
        try {
            PackageManager packageManager = APlayerApplication.getContext().getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(APlayerApplication.getContext().getPackageName(), PackageManager.GET_META_DATA);
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

}
