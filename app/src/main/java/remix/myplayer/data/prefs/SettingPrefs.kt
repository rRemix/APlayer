package remix.myplayer.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import remix.myplayer.helper.LanguageHelper.AUTO
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.Constants.MB
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingPrefsEntryPoint {

  fun settingPrefs(): SettingPrefs
}

@Singleton
class SettingPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, PrefKeys.Setting.NAME) {

  var firstLoad by PrefsDelegate(sp, PrefKeys.Setting.FIRST_LOAD, true)

  var libraryJson by PrefsDelegate(sp, PrefKeys.Setting.LIBRARY, "")

  var scanSize by PrefsDelegate(sp, PrefKeys.Setting.SCAN_SIZE, MB)

  var songSortOrder by PrefsDelegate(sp, PrefKeys.Setting.SONG_SORT_ORDER, SortOrder.SONG_A_Z)
  var albumSortOrder by PrefsDelegate(sp, PrefKeys.Setting.ALBUM_SORT_ORDER, SortOrder.ALBUM_A_Z)
  var artistSortOrder by PrefsDelegate(sp, PrefKeys.Setting.ARTIST_SORT_ORDER, SortOrder.ARTIST_A_Z)
  var playlistSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.PLAYLIST_SORT_ORDER,
    SortOrder.PLAYLIST_DATE
  )
  var genreSortOrder by PrefsDelegate(sp, PrefKeys.Setting.GENRE_SORT_ORDER, SortOrder.GENRE_A_Z)
  var folderSortOrder by PrefsDelegate(sp, PrefKeys.Setting.FOLDER_SORT_ORDER, SortOrder.FOLDER_A_Z)
  var historySortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.HISTORY_SORT_ORDER,
    SortOrder.PLAY_COUNT_DESC
  )

  var albumDetailSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.CHILD_ALBUM_SONG_SORT_ORDER,
    SortOrder.TRACK_NUMBER
  )
  var artistDetailSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.CHILD_ARTIST_SONG_SORT_ORDER,
    SortOrder.SONG_A_Z
  )

  @Deprecated("use getPlayListDetailSortOrder(playlistId) / setPlayListDetailSortOrder(playlistId, order) instead")
  var playListDetailSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.CHILD_PLAYLIST_SONG_SORT_ORDER,
    SortOrder.SONG_A_Z
  )
  var genreDetailSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.CHILD_GENRE_SONG_SORT_ORDER,
    SortOrder.SONG_A_Z
  )
  var folderDetailSortOrder by PrefsDelegate(
    sp,
    PrefKeys.Setting.CHILD_FOLDER_SONG_SORT_ORDER,
    SortOrder.SONG_A_Z
  )

  fun getPlayListDetailSortOrder(playlistId: Long): String {
    val key = playListDetailSortKey(playlistId)
    return sp.getString(key, null) ?: playListDetailSortOrder
  }

  fun setPlayListDetailSortOrder(playlistId: Long, sortOrder: String): Boolean {
    val key = playListDetailSortKey(playlistId)
    val current = sp.getString(key, null) ?: playListDetailSortOrder
    if (current == sortOrder) {
      return false
    }
    sp.edit(commit = true) {
      putString(key, sortOrder)
    }
    return true
  }

  private fun playListDetailSortKey(playlistId: Long): String {
    return PrefKeys.Setting.CHILD_PLAYLIST_SONG_SORT_ORDER_PREFIX + playlistId
  }

  var albumMode by PrefsDelegate(sp, PrefKeys.Setting.MODE_FOR_ALBUM, GRID_MODE)
  var artistMode by PrefsDelegate(sp, PrefKeys.Setting.MODE_FOR_ARTIST, GRID_MODE)
  var genreMode by PrefsDelegate(sp, PrefKeys.Setting.MODE_FOR_GENRE, GRID_MODE)
  var playlistMode by PrefsDelegate(sp, PrefKeys.Setting.MODE_FOR_PLAYLIST, GRID_MODE)

  var manualScanFolder by PrefsDelegate(sp, PrefKeys.Setting.MANUAL_SCAN_FOLDER, "")
  var deleteIds by PrefsDelegate(sp, PrefKeys.Setting.BLACKLIST_SONG, emptySet<String>())
  var blacklist by PrefsDelegate(sp, PrefKeys.Setting.BLACKLIST, emptySet<String>())
  var deleteSource by PrefsDelegate(sp, PrefKeys.Setting.DELETE_SOURCE, false)

  var lockScreen by PrefsDelegate(sp, PrefKeys.Setting.LOCKSCREEN, LOCKSCREEN_SYSTEM)
  var language by PrefsDelegate(sp, PrefKeys.Setting.LANGUAGE, AUTO)
  var uiFontScale by PrefsDelegate(
    sp,
    PrefKeys.Setting.UI_FONT_SCALE,
    UI_FONT_SCALE_DEFAULT
  )
  var playAtBreakPoint by PrefsDelegate(sp, PrefKeys.Setting.PLAY_AT_BREAKPOINT, false)
  var shake by PrefsDelegate(sp, PrefKeys.Setting.SHAKE, false)
  var showDisplayName by PrefsDelegate(sp, PrefKeys.Setting.SHOW_DISPLAYNAME, false)

  var ignoreAudioFocus by PrefsDelegate(sp, PrefKeys.Setting.AUDIO_FOCUS, false)
  var decoderMode by PrefsDelegate(
    sp,
    PrefKeys.Setting.AUDIO_DECODER_MODE,
    DECODER_MODE_DEFAULT
  )
  var autoPlay by PrefsDelegate(sp, PrefKeys.Setting.AUTO_PLAY, NEVER)
  var crossFade by PrefsDelegate(sp, PrefKeys.Setting.CROSS_FADE, false)
  var speed by PrefsDelegate(sp, PrefKeys.Setting.SPEED, "1.0")
  val speedValue get() = speed.toFloat()
  var playModel by PrefsDelegate(sp, PrefKeys.Setting.PLAY_MODEL, MODE_LOOP)
  var listLoop by PrefsDelegate(sp, PrefKeys.Setting.LIST_LOOP, true)
  var lastSong by PrefsDelegate(sp, PrefKeys.Setting.LAST_SONG, "")
  var lastProgress by PrefsDelegate(sp, PrefKeys.Setting.LAST_PLAY_PROGRESS, 0)

  var playingScreenBackground by PrefsDelegate(
    sp,
    PrefKeys.Setting.PLAYER_BACKGROUND,
    BACKGROUND_ADAPTIVE_COLOR
  )
  var playingCoverAnimationStyle by PrefsDelegate(
    sp,
    PrefKeys.Setting.PLAYING_COVER_ANIMATION_STYLE,
    COVER_ANIMATION_CLASSIC
  )
  var playingCoverAnimationSpeed by PrefsDelegate(
    sp,
    PrefKeys.Setting.PLAYING_COVER_ANIMATION_SPEED,
    COVER_ANIMATION_SPEED_DEFAULT
  )
  var playingScreenBottom by PrefsDelegate(
    sp,
    PrefKeys.Setting.BOTTOM_OF_NOW_PLAYING_SCREEN,
    BOTTOM_SHOW_BOTH
  )
  var keepScreenOn by PrefsDelegate(sp, PrefKeys.Setting.SCREEN_ALWAYS_ON, false)

  var ignoreMediaStore by PrefsDelegate(sp, PrefKeys.Setting.IGNORE_MEDIA_STORE, false)
  var autoDownloadCover by PrefsDelegate(
    sp,
    PrefKeys.Setting.AUTO_DOWNLOAD_ALBUM_COVER,
    DOWNLOAD_COVER_ALWAYS
  )
  var downloadSource by PrefsDelegate(
    sp,
    PrefKeys.Setting.ALBUM_COVER_DOWNLOAD_SOURCE,
    DOWNLOAD_NETEASE
  )

  var classicNotify by PrefsDelegate(sp, PrefKeys.Setting.NOTIFY_STYLE_CLASSIC, false)
  var notifyUseSystemBackground by PrefsDelegate(sp, PrefKeys.Setting.NOTIFY_SYSTEM_COLOR, true)

  var exitAfterTimerFinish by PrefsDelegate(sp, PrefKeys.Setting.TIMER_EXIT_AFTER_FINISH, false)
  var timerStartAuto by PrefsDelegate(sp, PrefKeys.Setting.TIMER_DEFAULT, false)
  var timerDefaultDuration by PrefsDelegate(sp, PrefKeys.Setting.TIMER_DURATION, -1)

  var bassBoostStrength by PrefsDelegate(sp, PrefKeys.Setting.BASS_BOOST_STRENGTH, 0)
  var enableEq by PrefsDelegate(sp, PrefKeys.Setting.ENABLE_EQ, false)

  var checkMigration16600 by PrefsDelegate(sp, "check_migration_16600", false)
  var checkMigration20500 by PrefsDelegate(sp, "check_migration_20500", false)
  var checkMigration21100 by PrefsDelegate(sp, "check_migration_21100", false)

  companion object {

    // 播放界面底部
    const val BOTTOM_SHOW_NEXT = 0
    const val BOTTOM_SHOW_VOLUME = 1
    const val BOTTOM_SHOW_BOTH = 2
    const val BOTTOM_SHOW_NONE = 3

    // 播放界面背景
    const val BACKGROUND_THEME = 0
    const val BACKGROUND_ADAPTIVE_COLOR = 1
    const val BACKGROUND_CUSTOM_IMAGE = 2

    // 播放页封面切换动画
    const val COVER_ANIMATION_CLASSIC = "classic"
    const val COVER_ANIMATION_PARALLAX_PUSH = "parallax_push"
    const val COVER_ANIMATION_CARD_SQUEEZE = "card_squeeze"
    const val COVER_ANIMATION_PAGE_TURN = "page_turn"
    const val COVER_ANIMATION_SLICE_STAGGER = "slice_stagger"
    const val COVER_ANIMATION_DISSOLVE_ZOOM = "dissolve_zoom"
    const val COVER_ANIMATION_SPEED_DEFAULT = 1.5f

    // 封面下载
    const val DOWNLOAD_COVER_ALWAYS = 0
    const val DOWNLOAD_COVER_WIFI_ONLY = 1
    const val DOWNLOAD_COVER_NEVER = 2

    const val CLASSIC_NOTIFY_BACKGROUND_SYSTEM = 0

    // 0:软件锁屏 1:系统锁屏 2:关闭
    const val LOCKSCREEN_APLAYER: Int = 0
    const val LOCKSCREEN_SYSTEM: Int = 1
    const val LOCKSCREEN_CLOSE: Int = 2

    // 播放模式
    const val MODE_LOOP: Int = 1
    const val MODE_SHUFFLE: Int = 2
    const val MODE_REPEAT: Int = 3

    // 音频解码方式
    const val DECODER_MODE_DEFAULT: Int = 0
    const val DECODER_MODE_FFMPEG: Int = 1

    // 自动播放
    const val HEADSET_PLUG = 0
    const val OPEN_SOFTWARE = 1
    const val NEVER = 2

    // 封面下载源
    const val DOWNLOAD_LASTFM = 0
    const val DOWNLOAD_NETEASE = 1

    const val LIST_MODE = 0
    const val GRID_MODE = 1

    const val UI_FONT_SCALE_DEFAULT = 1.0f
    const val UI_FONT_SCALE_MIN = 0.85f
    const val UI_FONT_SCALE_MAX = 1.5f
    const val UI_FONT_SCALE_STEP = 0.05f

    fun normalizeUiFontScale(scale: Float): Float {
      val snapped = (scale / UI_FONT_SCALE_STEP).roundToInt() * UI_FONT_SCALE_STEP
      return snapped.coerceIn(UI_FONT_SCALE_MIN, UI_FONT_SCALE_MAX)
    }
  }
}

fun SettingPrefs.playlistSortOrderFlow(): Flow<String> {
  return callbackFlow {
    trySend(playlistSortOrder)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == PrefKeys.Setting.PLAYLIST_SORT_ORDER) {
        trySend(playlistSortOrder)
      }
    }
    sp.registerOnSharedPreferenceChangeListener(listener)
    awaitClose {
      sp.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }.distinctUntilChanged()
}

fun SettingPrefs.historySortOrderFlow(): Flow<String> {
  return callbackFlow {
    trySend(historySortOrder)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == PrefKeys.Setting.HISTORY_SORT_ORDER) {
        trySend(historySortOrder)
      }
    }
    sp.registerOnSharedPreferenceChangeListener(listener)
    awaitClose {
      sp.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }.distinctUntilChanged()
}
