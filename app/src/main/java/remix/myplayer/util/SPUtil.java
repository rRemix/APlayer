package remix.myplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import remix.myplayer.bean.misc.LyricPriority;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * SharedPrefs工具类
 */
public class SPUtil {

  private static SPUtil mInstance;

  public SPUtil() {
    if (mInstance == null) {
      mInstance = this;
    }
  }

  public static void putStringSet(Context context, String name, String key, Set<String> set) {
    if (set == null) {
      return;
    }
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.remove(key);
    editor.putStringSet(key, set).apply();
  }

  public static Set<String> getStringSet(Context context, String name, String key) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .getStringSet(key, new HashSet<>());
  }

  public static void putValue(Context context, String name, String key, int value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.putInt(key, value).apply();
  }

  public static void putValue(Context context, String name, String key, long value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.putLong(key, value).apply();
  }

  public static void putValue(Context context, String name, String key, String value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.putString(key, value).apply();
  }

  public static void putValue(Context context, String name, String key, boolean value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();

    editor.putBoolean(key, value).apply();
  }

  public static boolean getValue(Context context, String name, Object key, boolean dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getBoolean(key.toString(), dft);
  }

  public static int getValue(Context context, String name, Object key, int dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getInt(key.toString(), dft);
  }

  public static long getValue(Context context, String name, Object key, long dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getLong(key.toString(), dft);
  }

  public static String getValue(Context context, String name, Object key, String dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key.toString(), dft);
  }

  public static void deleteValue(Context context, String name, String key) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.remove(key).apply();
  }

  public static void deleteFile(Context context, String name) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.clear().apply();
  }

  public interface UPDATE_KEY {

    String NAME = "Update";

    String IGNORE_FOREVER = "ignore_forever";
  }

  public interface LYRIC_KEY {

    String NAME = "Lyric";
    //歌词搜索优先级
    String PRIORITY_LYRIC = "priority_lyric";
    String PRIORITY_LYRIC_v1 = "priority_lyric_v1";
    String DEFAULT_PRIORITY = new Gson().toJson(Arrays
            .asList(
                LyricPriority.EMBEDDED,
                LyricPriority.LOCAL,
                LyricPriority.KUGOU,
                LyricPriority.NETEASE,
                LyricPriority.QQ,
                LyricPriority.IGNORE),
        new TypeToken<List<LyricPriority>>() {
        }.getType());

    int LYRIC_DEFAULT = LyricPriority.DEF.getPriority();
    int LYRIC_IGNORE = LyricPriority.IGNORE.getPriority();
    int LYRIC_NETEASE = LyricPriority.NETEASE.getPriority();
    int LYRIC_KUGOU = LyricPriority.KUGOU.getPriority();
    int LYRIC_QQ = LyricPriority.QQ.getPriority();
    int LYRIC_LOCAL = LyricPriority.LOCAL.getPriority();
    int LYRIC_EMBEDDED = LyricPriority.EMBEDDED.getPriority();
    int LYRIC_MANUAL = LyricPriority.MANUAL.getPriority();

    String LYRIC_FONT_SIZE = "lyric_font_size";
    String LYRIC_RESET_ON_16000 = "lyric_reset_on_16000";
    String LYRIC_LOCAL_TIP_SHOWN = "lyric_local_tip_shown";
  }

  public interface COVER_KEY {

    String NAME = "Cover";
  }

  public interface SETTING_KEY {

    String NAME = "Setting";
    //第一次读取数据
    String FIRST_LOAD = "first_load";
    //桌面歌词是否可移动
    String DESKTOP_LYRIC_LOCK = "desktop_lyric_lock";
    //桌面歌词字体大小
    String DESKTOP_LYRIC_TEXT_SIZE = "desktop_lyric_text_size";
    //桌面歌词y坐标
    String DESKTOP_LYRIC_Y = "desktop_lyric_y";
    //是否开启屏幕常亮
    String SCREEN_ALWAYS_ON = "key_screen_always_on";
    //通知栏是否启用经典样式
    String NOTIFY_STYLE_CLASSIC = "notify_classic";
    //是否自动下载专辑封面
    String AUTO_DOWNLOAD_ALBUM_COVER = "auto_download_album_cover";
    //是否自动下载艺术家封面
    String AUTO_DOWNLOAD_ARTIST_COVER = "auto_download_artist_cover";
    //曲库配置
    String LIBRARY = "library_category";
    //锁屏设置
    String LOCKSCREEN = "lockScreen";
    //导航浪变色
    String COLOR_NAVIGATION = "color_Navigation";
    //摇一摇
    String SHAKE = "shake";
    //优先搜索在线歌词
    String ONLINE_LYRIC_FIRST = "online_lyric_first";
    //是否开启桌面歌词
    String DESKTOP_LYRIC_SHOW = "desktop_lyric_show";
    //是否开启状态栏歌词
    String STATUSBAR_LYRIC_SHOW = "statusbar_lyric_show";
    //沉浸式状态栏
    String IMMERSIVE_MODE = "immersive_mode";
    //过滤大小
    String SCAN_SIZE = "scan_size";
    //强制按拼音排序
    String FORCE_SORT = "force_sort";
    //歌曲排序顺序
    String SONG_SORT_ORDER = "song_sort_order";
    //专辑排序顺序
    String ALBUM_SORT_ORDER = "album_sort_order";
    //艺术家排序顺序
    String ARTIST_SORT_ORDER = "artist_sort_order";
    //播放列表排序顺序
    String PLAYLIST_SORT_ORDER = "playlist_sort_order";
    //流派排序
    String GENRE_SORT_ORDER = "genre_sort_order";
    //文件夹内歌曲排序顺序
    String CHILD_FOLDER_SONG_SORT_ORDER = "child_folder_song_sort_order";
    //艺术家内歌曲排序顺序
    String CHILD_ARTIST_SONG_SORT_ORDER = "child_artist_sort_order";
    //专辑内歌曲排序顺序
    String CHILD_ALBUM_SONG_SORT_ORDER = "child_album_song_sort_order";
    //播放列表内歌曲排序顺序
    String CHILD_PLAYLIST_SONG_SORT_ORDER = "child_playlist_song_sort_order";
    //流派内歌曲排序顺序
    String CHILD_GENRE_SONG_SORT_ORDER = "child_genre_song_sort_order";
    //播放次数排序
    String HISTORY_SORT_ORDER = "history_sort_order";
    //移除歌曲
    String BLACKLIST_SONG = "black_list_song";
    //黑名单
    String BLACKLIST = "blacklist";
    //本地歌词搜索路径
    String LOCAL_LYRIC_SEARCH_DIR = "local_lyric_search_dir";
    //退出时播放时间
    String LAST_PLAY_PROGRESS = "last_play_progress";
    //退出时播放的歌曲
    @Deprecated
    String LAST_SONG_ID = "last_song_id";
    //退出时播放的歌曲
    String LAST_SONG = "last_song";
    //退出时下一首歌曲
    //播放模式
    String PLAY_MODEL = "play_model";
    //经典通知栏背景是否是系统背景色
    String NOTIFY_SYSTEM_COLOR = "notify_system_color";
    //断点播放
    String PLAY_AT_BREAKPOINT = "play_at_breakpoint";
    //是否忽略媒体缓存
    String IGNORE_MEDIA_STORE = "ignore_media_store";
    //桌面部件样式
    String APP_WIDGET_SKIN = "app_widget_transparent";
    //是否默认开启定时器
    String TIMER_DEFAULT = "timer_default";
    //定时器时长
    String TIMER_DURATION = "timer_duration";
    //定时结束后等待当前歌曲播放完毕
    String TIMER_EXIT_AFTER_FINISH = "timer_exit_after_finish";
    //封面下载源
    String ALBUM_COVER_DOWNLOAD_SOURCE = "album_cover_download_source";
    //播放界面底部显示
    String BOTTOM_OF_NOW_PLAYING_SCREEN = "bottom_of_now_playing_screen";
    //倍速播放
    String SPEED = "speed";
    //移除是否同时源文件
    String DELETE_SOURCE = "delete_source";
    //是否保存日志文件到sd卡
    String WRITE_LOG_TO_STORAGE = "write_log_to_storage";
    //列表歌曲名是否取代为文件夹名
    String SHOW_DISPLAYNAME = "show_displayname";
    //专辑列表的显示模式
    String MODE_FOR_ALBUM = "mode_for_album";
    //艺术家列表的显示模式
    String MODE_FOR_ARTIST = "mode_for_artist";
    //流派列表的显示模式
    String MODE_FOR_GENRE = "mode_for_genre";
    //播放列表的显示模式
    String MODE_FOR_PLAYLIST = "mode_for_playlist";
    //语言
    String LANGUAGE = "language";
    //eq
    String ENABLE_EQ = "enable_eq";
    //bass boost
//    String ENABLE_BASS_BOOST = "enable_bass_boost";
    //bass boost strength
    String BASS_BOOST_STRENGTH = "bass_boost_strength";
    //virtualizer
//    String ENABLE_VIRTUALIZER = "enable_virtualizer";
    //virtualizer strength
    String VIRTUALIZER_STRENGTH = "virtualizer_strength";
    //音频焦点
    String AUDIO_FOCUS = "audio_focus";
    //自动播放
    String AUTO_PLAY = "auto_play_headset_plug_in";
    //手动扫描目录
    String MANUAL_SCAN_FOLDER = "manual_scan_folder";
    //自定义播放背景
    String PLAYER_BACKGROUND = "player_background";
    // 深色主题
    String DARK_THEME = "dark_theme";
    // 黑色主题
    String BLACK_THEME = "black_theme";
    String VERSION = "version";
    // 淡入淡出
    String CROSS_FADE = "cross_fade";
    int NEWEST_VERSION = 3;
  }

  public interface LYRIC_OFFSET_KEY {

    String NAME = "LyricOffset";
  }

  public interface OTHER_KEY {
    String NAME = "Other";

    String LAST_OPEN_TIME = "last_open_time";
    String WAS_SUPPORT = "was_support";
    String SUPPORT_NO_MORE_PROMPT = "support_no_more_prompt";
  }
}
