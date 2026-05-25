package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import remix.myplayer.data.model.misc.LyricOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通用的歌词配置，桌面歌词的配置在DesktopLyricPrefs
 * @see remix.myplayer.data.prefs.DesktopLyricPrefs
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
  var statusBarLyricEnabled by PrefsDelegate(sp, KEY_STATUS_BAR_LYRIC_ENABLED, false)

  var fontScale by PrefsDelegate(sp, KEY_LYRIC_FONT_SCALE, 1.0f)

  var translationEnabled by PrefsDelegate(sp, KEY_TRANSLATION_ENABLED, true)

  /**
   * 清除所有针对单首歌曲的配置
   */
  fun clearUserSave() {
    val keys = sp.all.keys
    val toRemove = keys.filter {
      it.startsWith(KEY_SONG_PREFIX) || it.startsWith(KEY_OFFSET_PREFIX)
    }
    if (toRemove.isEmpty()) return
    sp.edit(commit = true) {
      toRemove.forEach { remove(it) }
    }
  }

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
    const val KEY_DESKTOP_LYRIC_ENABLED = "desktop_lyric_enabled"
    const val KEY_DESKTOP_LYRIC_LOCKED = "desktop_lyric_locked"

    // LyricScreen
    const val KEY_LYRIC_FONT_SCALE = "lyric_font_scale"
    const val KEY_LYRIC_LOCAL_TIP_SHOWN = "lyric_local_tip_shown"
    const val KEY_GENERAL_LYRIC_ORDER = "general_lyric_order"

    const val KEY_TRANSLATION_ENABLED = "translation_enabled"

    const val KEY_SONG_PREFIX = "lyric_song_"
    const val KEY_OFFSET_PREFIX = "lyric_offset_"
  }
}