package remix.myplayer.data.prefs

/**
 * 统一管理 SharedPreferences 的键名。
 */
object PrefKeys {

  object Setting : Keys() {

    /** Setting 文件名 */
    const val NAME = "Setting"

    /** 第一次读取数据 */
    const val FIRST_LOAD = "first_load"

    /** 是否开启屏幕常亮 */
    const val SCREEN_ALWAYS_ON = "key_screen_always_on"

    /** 通知栏是否启用经典样式 */
    const val NOTIFY_STYLE_CLASSIC = "notify_classic"

    /** 是否自动下载专辑封面 */
    const val AUTO_DOWNLOAD_ALBUM_COVER = "auto_download_album_cover_v1"

    /** 曲库配置 */
    const val LIBRARY = "library_category"

    /** 锁屏设置 */
    const val LOCKSCREEN = "lockScreen"

    /** 摇一摇 */
    const val SHAKE = "shake"

    /** 是否开启桌面歌词 */
    const val DESKTOP_LYRIC_SHOW = "desktop_lyric_show"

    /** 是否开启状态栏歌词 */
    const val STATUSBAR_LYRIC_SHOW = "statusbar_lyric_show"

    /** 沉浸式状态栏 */
    const val IMMERSIVE_MODE = "immersive_mode"

    /** 过滤大小 */
    const val SCAN_SIZE = "scan_size"

    /** 强制按拼音排序 */
    const val FORCE_SORT = "force_sort"

    /** 歌曲排序顺序 */
    const val SONG_SORT_ORDER = "song_sort_order"

    /** 专辑排序顺序 */
    const val ALBUM_SORT_ORDER = "album_sort_order"

    /** 艺术家排序顺序 */
    const val ARTIST_SORT_ORDER = "artist_sort_order"

    /** 播放列表排序顺序 */
    const val PLAYLIST_SORT_ORDER = "playlist_sort_order"

    /** 流派排序 */
    const val GENRE_SORT_ORDER = "genre_sort_order"

    /** 文件夹排序顺序 */
    const val FOLDER_SORT_ORDER = "folder_sort_order"

    /** 文件夹内歌曲排序顺序 */
    const val CHILD_FOLDER_SONG_SORT_ORDER = "child_folder_song_sort_order"

    /** 艺术家内歌曲排序顺序 */
    const val CHILD_ARTIST_SONG_SORT_ORDER = "child_artist_sort_order"

    /** 专辑内歌曲排序顺序 */
    const val CHILD_ALBUM_SONG_SORT_ORDER = "child_album_song_sort_order"

    /** 播放列表内歌曲排序顺序 */
    const val CHILD_PLAYLIST_SONG_SORT_ORDER = "child_playlist_song_sort_order"
    const val CHILD_PLAYLIST_SONG_SORT_ORDER_PREFIX = "child_playlist_song_sort_order_"

    /** 流派内歌曲排序顺序 */
    const val CHILD_GENRE_SONG_SORT_ORDER = "child_genre_song_sort_order"

    /** 播放次数排序 */
    const val HISTORY_SORT_ORDER = "history_sort_order"

    /** 移除歌曲 */
    const val BLACKLIST_SONG = "black_list_song"

    /** 黑名单 */
    const val BLACKLIST = "blacklist"

    /** 退出时播放时间 */
    const val LAST_PLAY_PROGRESS = "last_play_progress"

    /** 退出时播放的歌曲 */
    const val LAST_SONG = "last_song"

    /** 播放模式 */
    const val PLAY_MODEL = "play_model"

    /** 列表是否循环 */
    const val LIST_LOOP = "list_loop"

    /** 经典通知栏背景是否是系统背景色 */
    const val NOTIFY_SYSTEM_COLOR = "notify_system_color"

    /** 断点播放 */
    const val PLAY_AT_BREAKPOINT = "play_at_breakpoint"

    /** 是否忽略媒体缓存 */
    const val IGNORE_MEDIA_STORE = "ignore_media_store"

    /** 桌面部件样式 */
    const val APP_WIDGET_SKIN = "app_widget_transparent"

    /** 是否默认开启定时器 */
    const val TIMER_DEFAULT = "timer_default"

    /** 定时器时长 */
    const val TIMER_DURATION = "timer_duration"

    /** 定时结束后等待当前歌曲播放完毕 */
    const val TIMER_EXIT_AFTER_FINISH = "timer_exit_after_finish"

    /** 封面下载源 */
    const val ALBUM_COVER_DOWNLOAD_SOURCE = "album_cover_download_source"

    /** 播放界面底部显示 */
    const val BOTTOM_OF_NOW_PLAYING_SCREEN = "bottom_of_now_playing_screen"

    /** 倍速播放 */
    const val SPEED = "speed"

    /** 移除是否同时源文件 */
    const val DELETE_SOURCE = "delete_source"

    /** 列表歌曲名是否取代为文件夹名 */
    const val SHOW_DISPLAYNAME = "show_displayname"

    /** 专辑列表的显示模式 */
    const val MODE_FOR_ALBUM = "mode_for_album"

    /** 艺术家列表的显示模式 */
    const val MODE_FOR_ARTIST = "mode_for_artist"

    /** 流派列表的显示模式 */
    const val MODE_FOR_GENRE = "mode_for_genre"

    /** 播放列表的显示模式 */
    const val MODE_FOR_PLAYLIST = "mode_for_playlist"

    /** 语言 */
    const val LANGUAGE = "language"

    /** 界面字体缩放 */
    const val UI_FONT_SCALE = "ui_font_scale"

    /** EQ */
    const val ENABLE_EQ = "enable_eq"

    /** Bass Boost 强度 */
    const val BASS_BOOST_STRENGTH = "bass_boost_strength"

    /** Virtualizer 强度 */
    const val VIRTUALIZER_STRENGTH = "virtualizer_strength"

    /** 音频焦点 */
    const val AUDIO_FOCUS = "audio_focus"

    /** 音频解码方式 */
    const val AUDIO_DECODER_MODE = "audio_decoder_mode"

    /** 自动播放 */
    const val AUTO_PLAY = "auto_play_headset_plug_in"

    /** 手动扫描目录 */
    const val MANUAL_SCAN_FOLDER = "manual_scan_folder"

    /** 自定义播放背景 */
    const val PLAYER_BACKGROUND = "player_background"

    /** 播放页封面切换动画 */
    const val PLAYING_COVER_ANIMATION_STYLE = "playing_cover_animation_style"

    /** 播放页封面切换动画速率 */
    const val PLAYING_COVER_ANIMATION_SPEED = "playing_cover_animation_speed"

    /** 版本号 */
    const val VERSION = "version"

    /** 淡入淡出 */
    const val CROSS_FADE = "cross_fade"

    override val latestVersion = 3
  }

  object Theme : Keys() {
    const val NAME = "aplayer-theme"
    const val PRIMARY_COLOR = "primary_color"
    const val SECONDARY_COLOR = "accent_color"
    const val DARK_THEME = "dark_theme"
    const val BLACK_THEME = "black_theme"
    const val COLOR_NAVIGATION = "color_navigation"

    override val latestVersion = 1
  }


  sealed class Keys {

    abstract val latestVersion: Int

    companion object {
      const val KEY_VERSION = "key_version"
    }
  }
}
