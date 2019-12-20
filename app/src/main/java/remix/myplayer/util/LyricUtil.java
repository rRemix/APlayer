package remix.myplayer.util;

import android.os.Environment;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class LyricUtil {

  private static final String TAG = "LyricUtil";

  private LyricUtil() {
  }


  public static String searchLyric(String displayName, String songName, String artistName,
      File searchPath) {
    List<String> paths = new ArrayList<>();
    searchLyricInternal(paths, displayName, songName, artistName, searchPath);
    return paths.size() > 0 ? paths.get(0) : "";
  }

  /**
   * 查找歌曲的lrc文件
   */
  private static void searchLyricInternal(List<String> result, String displayName, String songName,
      String artistName, File searchPath) {
    //判断SD卡是否存在
    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      File[] files = searchPath.listFiles();
      if (files == null || files.length == 0) {
        return;
      }
      for (File file : files) {
        if (file.isDirectory() && file.canRead()) {
          searchLyricInternal(result, displayName, songName, artistName, file);
        } else {
          if (isRightLrc(file, displayName, songName, artistName)) {
            result.add(file.getAbsolutePath());
            return;
          }
        }
      }
    }

  }

  /**
   * 判断是否是相匹配的歌词
   */
  public static boolean isRightLrc(File file, String displayName, String title, String artist) {
//        BufferedReader br = null;
    try {
      if (file == null || !file.canRead() || !file.isFile()) {
        return false;
      }
      if (TextUtils.isEmpty(file.getAbsolutePath()) || TextUtils.isEmpty(displayName) ||
          TextUtils.isEmpty(title) || TextUtils.isEmpty(artist)) {
        return false;
      }
      //仅判断.lrc文件
      if (!file.getName().endsWith("lrc")) {
        return false;
      }
      //暂时忽略网易云的歌词
      if (file.getAbsolutePath().contains("netease/cloudmusic/")) {
        return false;
      }
      String fileName = file.getName().indexOf('.') > 0 ?
          file.getName().substring(0, file.getName().lastIndexOf('.')) : file.getName();
      //判断歌词文件名与歌曲文件名是否一致
      if (fileName.equalsIgnoreCase(displayName)) {
        return true;
      }
      //判断是否包含歌手名和歌曲名
      if (fileName.toUpperCase().contains(title.toUpperCase()) && fileName.toUpperCase()
          .contains(artist.toUpperCase())) {
        return true;
      }
      //读取前五行歌词内容进行判断
//            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharset(file.getAbsolutePath())));
//            boolean hasArtist = false;
//            boolean hasTitle = false;
//            for (int i = 0; i < 5; i++) {
//                String lrcLine;
//                if ((lrcLine = br.readLine()) == null)
//                    break;
//                if (lrcLine.contains("ar") && lrcLine.equalsIgnoreCase(artist)) {
//                    hasArtist = true;
//                    continue;
//                }
//                if (lrcLine.contains("ti") && lrcLine.equalsIgnoreCase(title)) {
//                    hasTitle = true;
//                }
//            }
//            if (hasArtist && hasTitle) {
//                return true;
//            }
    } catch (Exception e) {

    } finally {
//            try {
//                if (br != null) {
//                    br.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
    }
    return false;
  }

  public static String getCharset(final String filePath) {
    try {
      return EncodingDetect.getJavaEncode(filePath);
    } catch (Exception e) {
      Timber.w(e);
      return "UTF-8";
    }
  }
}
