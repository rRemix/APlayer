package remix.myplayer.compose.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import remix.myplayer.bean.misc.LyricOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通用的歌词配置，桌面歌词的配置在DesktopLyricPrefs
 * @see remix.myplayer.ui.widget.desktop.DesktopLyricPrefs
 */
@Singleton
class LyricPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "Lyric") {

  // 默认的歌词搜索顺序
  val generalLyricOrderList: List<LyricOrder>
    get() = try {
      Json.decodeFromString<List<LyricOrder>>(generalLyricOrder)
    } catch (ignore: Exception) {
      defaultLyricOrderList
    }

  var generalLyricOrder by PrefsDelegate(
    sp,
    KEY_GENERAL_LYRIC_ORDER,
    Json.encodeToString(defaultLyricOrderList)
  )

  var tipShown by PrefsDelegate(sp, KEY_LYRIC_LOCAL_TIP_SHOWN, false)

  var desktopLyricEnabled by PrefsDelegate(sp, KEY_DESKTOP_LYRIC_ENABLED, false)
  var desktopLyricLocked by PrefsDelegate(sp, KEY_DESKTOP_LYRIC_LOCKED, false)
  var statusBarLyricEnabled by PrefsDelegate(sp, KEY_STATUS_BAR_LYRIC_ENABLED, false)

  var fontScale by PrefsDelegate(sp, KEY_LYRIC_FONT_SCALE, 1.0f)

  companion object {

    private val defaultLyricOrderList = listOf(
      LyricOrder.Embedded,
      LyricOrder.Local,
      LyricOrder.Kugou,
      LyricOrder.Netease,
      LyricOrder.Qq,
      LyricOrder.Ignore
    )

    // StatusBar
    const val KEY_STATUS_BAR_LYRIC_ENABLED: String = "status_bar_lyric_enabled"

    // Desktop
    const val KEY_DESKTOP_LYRIC_ENABLED: String = "desktop_lyric_enabled"
    const val KEY_DESKTOP_LYRIC_LOCKED: String = "desktop_lyric_locked"

    // LyricScreen
    const val KEY_LYRIC_FONT_SCALE: String = "lyric_font_scale"
    const val KEY_LYRIC_LOCAL_TIP_SHOWN: String = "lyric_local_tip_shown"
    const val KEY_GENERAL_LYRIC_ORDER = "general_lyric_order"
    const val KEY_SONG_PREFIX = "lyric_song_"
    const val KEY_OFFSET_PREFIX = "lyric_offset_"
  }
}