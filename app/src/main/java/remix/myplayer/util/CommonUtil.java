package remix.myplayer.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.model.Feedback;
import remix.myplayer.util.thumb.SearchCover;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 通用工具类
 */
public class CommonUtil {
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    /**
     * 震动
     * @param context
     * @param milliseconds
     */
    public static void Vibrate(final Context context,final long milliseconds) {
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
            file.renameTo(tmp);
            return tmp.delete();
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
            } finally {
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
        boolean s = file.exists() && file.delete();
        return s;
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
                return mContext.getString(R.string.unknow_song);
            } else {
//                return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
                return origin;
            }
        } else{
            if(origin == null || origin.equals("") || origin.contains("unknown") ){
                return mContext.getString(type == ARTISTTYPE ? R.string.unknow_artist : R.string.unknow_album);
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
    public static  <T extends Object> String getMapkeyByPosition(Map<String,ArrayList<T>> map, int position){
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
     * 根据专辑id返回封面的url
     * @param albumId
     * @return
     */
    public static JSONObject getCoverJsonObject(int albumId){
        BufferedReader br = null;
        StringBuffer strBuffer = new StringBuffer();
        String s;
        try {
            URL albumUrl = new URL("http://geci.me/api/cover/"  + albumId);
            HttpURLConnection httpURLConnection = (HttpURLConnection)albumUrl.openConnection();
            httpURLConnection.connect();
            InputStreamReader inReader = new InputStreamReader(httpURLConnection.getInputStream());
            br = new BufferedReader(inReader);
            if(br == null)
                return null;
            while((s = br.readLine()) != null){
                strBuffer.append(s);
            }
            return new JSONObject(strBuffer.toString());

        }  catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e){
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
     * 判断某个专辑在本地数据库是否有封面
     * @param uri
     * @return
     */
    public static boolean isAlbumThumbExistInDB(Uri uri){
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
     * 下载专辑封面并根据专辑id保存
     * @param songname
     * @param artist
     * @param albumid
     */
    public static String downAlbumCover(String songname,String artist,long albumid){
        String urlstr = new SearchCover(songname,artist,SearchCover.COVER).getImgUrl();
        URL url = null;
        BufferedReader br = null;
        StringBuffer strBuffer = new StringBuffer();
        FileOutputStream fos = null;
        InputStream in = null;
        String s;
        File img = null;
        try {
            url = new URL(urlstr);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            in = httpURLConnection.getInputStream();

            if(in == null || !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return "";

            File dir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/cache/cover");
            if(!dir.exists())
                dir.mkdirs();
            img = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/cache/cover/" + albumid  + ".jpg");
            fos = new FileOutputStream(img);

            byte[] bs = new byte[1024];
            int len = 0;
            // 开始读取
            while ((len = in.read(bs)) != -1) {
                fos.write(bs);
            }
            if(fos != null)
                fos.flush();

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if(in != null)
                    in.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        return img == null ? "" : img.getAbsolutePath();
    }

    public static String getCoverInCache(long albumId){
        File coverCacheDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/cache/cover");
        if(coverCacheDir.isDirectory()){
            File files[] = coverCacheDir.listFiles();
            if(files != null){
                for (File f : files){
                    if(f.getName().equals(albumId + "")){
                        return f.getAbsolutePath();
                    }
                }
            }
        }
        return "";
    }

    /**
     * 保存所有名字包含lyric的目录
     */
    public static void getLyricDir(File searchFile){

        //判断SD卡是否存在
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File[] files = searchFile.listFiles();
            if(files == null || files.length == 0)
                return;
            for (File file : files) {
                if (file.isDirectory()) {
                    if(file.canRead() && file.getName().contains("lyric")){
                        //保存
                        Global.LyricDir.add(file);
                    }
                    getLyricDir(file);
                }
            }
        }
    }

    /**
     * 查找歌曲的lrc文件
     * @param context
     * @param songName
     * @param searchPath
     */
    public static void searchFile(Context context,String displayName,String songName,String artistName,File searchPath) {
        //判断SD卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = searchPath.listFiles();
            if(files == null || files.length == 0)
                return;
            for(File file : files){
                if (file.isDirectory()){
                    if(file.canRead()){
                        searchFile(context,displayName,songName,artistName,file);
                    }
                } else {
                    if(isRightLrc(file,displayName,songName,artistName)){
                        Global.CurrentLrcPath = file.getAbsolutePath();
                        return;
                    }
                }
            }
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    //如果目录可读就执行（一定要加，不然会挂掉）
//                    if(file.canRead()){
//                        searchFile(context,songName,artistName,file);  //如果是目录，递归查找
//                    }
//                } else {
//                    //判断是文件
//                    BufferedReader br = null;
//                    try {
//                        br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//                        String prefix = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(".") + 1);
//                        String fileName = file.getName();
//                        if(prefix.equals("lrc") ){
//                            //先判断是否包含歌手名和歌曲名
//                            if(fileName.toUpperCase().contains(songName.toUpperCase()) &&
//                                    fileName.toUpperCase().contains(artistName.toUpperCase())){
//                                Global.CurrentLrcPath = file.getAbsolutePath();
//                                LogUtil.d("Lrc","LrcPath:" + Global.CurrentLrcPath);
//                            }
//                            //读取前五行歌词内容进行判断
//                            String lrcLine = "";
//                            boolean hasArtist = false;
//                            boolean hasTitle = false;
//                            for(int i = 0 ; i < 5;i++){
//                                if((lrcLine = br.readLine()) == null)
//                                    break;
//                                LogUtil.d("LrcLine","LrcLine:" + lrcLine);
//                                if(lrcLine.contains(artistName))
//                                    hasArtist = true;
//                                if(lrcLine.contains(songName))
//                                    hasTitle = true;
//                            }
//                            if(hasArtist && hasTitle){
//                                Global.CurrentLrcPath = file.getAbsolutePath();
//                                LogUtil.d("Lrc","LrcPath:" + Global.CurrentLrcPath);
//                                return;
//                            }
//                        }
//                    } catch(Exception e) {
//                        uploadException("查找歌词文件错误",e);
//                    } finally {
//                        try {
//                            if(br != null){
//                                br.close();
//                            }
//                        } catch (Exception e){
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
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
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String prefix = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(".") + 1);
            String fileName = file.getName().substring(0,file.getName().lastIndexOf('.'));
            if(prefix.equals("lrc") ){
                //判断歌词文件名与歌曲文件名是否一致
                if(fileName.toUpperCase().equals(displayName.toUpperCase())) {
                    return true;
                }
                //判断是否包含歌手名和歌曲名
                if(fileName.contains(title) || fileName.contains(title.toUpperCase())
                        && (fileName.contains(artist) || fileName.contains(artist.toUpperCase()))){
                    return true;
                }
                if(fileName.toUpperCase().contains(title.toUpperCase()) && fileName.toUpperCase().contains(artist.toUpperCase())){
                    return true;
                }
                //读取前五行歌词内容进行判断
                String lrcLine = "";
                boolean hasArtist = false;
                boolean hasTitle = false;
                for(int i = 0 ; i < 5;i++){
                    if((lrcLine = br.readLine()) == null)
                        break;
                    if(lrcLine.contains("ar") && lrcLine.toUpperCase().contains(artist.toUpperCase()) )
                        hasArtist = true;
                    if(lrcLine.contains("ti") && lrcLine.toUpperCase().contains(title.toUpperCase()))
                        hasTitle = true;
                }
                if(hasArtist && hasTitle){
                    return true;
                }
            }
        } catch (Exception e) {
            CommonUtil.uploadException("查找本地歌词错误",e);
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
    public static void uploadException(String title,String content){
        try {
            if(!CommonUtil.isNetWorkConnected()){
                return;
            }
            PackageManager pm = APlayerApplication.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(APlayerApplication.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Feedback feedback =  new Feedback(content,
                    title,
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + ""
            );
            feedback.save(new SaveListener<String>() {
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
            if(!CommonUtil.isNetWorkConnected()){
                return;
            }
            PackageManager pm = APlayerApplication.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(APlayerApplication.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Feedback feedback =  new Feedback(exception.toString(),
                    title,
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + ""
            );
            feedback.save(new SaveListener<String>() {
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
}
