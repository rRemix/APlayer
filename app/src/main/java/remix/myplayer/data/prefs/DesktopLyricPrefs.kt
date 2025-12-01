package remix.myplayer.data.prefs

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 桌面歌词内部的状态
 */
@Singleton
class DesktopLyricPrefs @Inject constructor(@ApplicationContext context: Context) :
  AbstractPref(context, "DesktopLyric") {

  var locked by PrefsDelegate(sp, LOCKED, false)
  var firstLineSize by PrefsDelegate(sp, FIRST_LINE_SIZE, DEFAULT_FIRST_LINE_SIZE)
  var secondLineSize by PrefsDelegate(sp, SECOND_LINE_SIZE, DEFAULT_SECOND_LINE_SIZE)
  var sungColor by PrefsDelegate(sp, SUNG_COLOR, DEFAULT_SUNG_COLOR)

  var unSungColor by PrefsDelegate(sp, UNSUNG_COLOR, DEFAULT_UNSUNG_COLOR)
  var translationColor by PrefsDelegate(sp, TRANSLATION_COLOR, DEFAULT_TRANSLATION_COLOR)

  companion object {

    internal const val ELLIPSIS = Typography.ellipsis.toString()

    internal const val HIDE_PANEL_DELAY = 3000L

    internal const val DEFAULT_FIRST_LINE_SIZE = 18f
    internal const val DEFAULT_SECOND_LINE_SIZE = 16f

    @ColorInt
    internal const val DEFAULT_SUNG_COLOR = Color.TRANSPARENT

    @ColorInt
    internal const val DEFAULT_UNSUNG_COLOR = 0xffffffff.toInt()

    @ColorInt
    internal const val DEFAULT_TRANSLATION_COLOR = 0xffffffff.toInt()

    private const val LOCKED: String = "locked"
    const val Y_POSITION_PREFIX: String = "y_position_" // y_position_$orientation
    private const val FIRST_LINE_SIZE: String = "first_line_size"
    private const val SECOND_LINE_SIZE: String = "second_line_size"
    private const val SUNG_COLOR: String = "sung_color"
    private const val UNSUNG_COLOR: String = "unsung_color"
    private const val TRANSLATION_COLOR: String = "translation_color"
//    const val UNSUNG_COLOR: String = "unsung_color"
//    const val TRANSLATION_COLOR: String = "translation_color"
  }

}