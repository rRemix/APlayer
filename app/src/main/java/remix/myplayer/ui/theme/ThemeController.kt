package remix.myplayer.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import remix.myplayer.data.prefs.ThemePrefs
import remix.myplayer.data.prefs.ThemePrefs.Companion.FOLLOW_SYSTEM
import javax.inject.Inject

val LocalThemeController = compositionLocalOf<ThemeController> {
  error("no controller provide")
}

interface ThemeController {

  val appTheme: AppTheme

  var dark: String

  var black: Boolean

  fun onSystemThemeChanged(uiMode: Int)

  fun setColoredNaviBar(colored: Boolean)

  fun setPrimary(color: Color)

  fun setSecondary(color: Color)
}

class ThemeControllerImpl @Inject constructor(private val storage: ThemePrefs) : ThemeController {

  private val _currentTheme: MutableState<AppTheme> = mutableStateOf(
    AppTheme(
      primary = Color(storage.primaryColor),
      secondary = Color(storage.secondaryColor),
      theme = storage.resolveTheme(storage.darkTheme, storage.blackTheme),
      coloredNaviBar = storage.coloredNaviBar
    )
  )

  override val appTheme: AppTheme
    get() = _currentTheme.value

  override var dark: String = FOLLOW_SYSTEM
    get() = storage.darkTheme
    set(value) {
      if (field == value) {
        return
      }
      field = value
      storage.darkTheme = value
      _currentTheme.value = _currentTheme.value.copy(
        theme = storage.resolveTheme(value, storage.blackTheme)
      )
    }

  override var black: Boolean = false
    get() = storage.blackTheme
    set(value) {
      if (field == value) {
        return
      }
      field = value
      storage.blackTheme = value
      _currentTheme.value = _currentTheme.value.copy(
        theme = storage.resolveTheme(storage.darkTheme, value)
      )
    }

  override fun onSystemThemeChanged(uiMode: Int) {
    if (storage.darkTheme != FOLLOW_SYSTEM) {
      return
    }
    val resolved = storage.resolveTheme(storage.darkTheme, storage.blackTheme, uiMode)
    if (_currentTheme.value.theme == resolved) {
      return
    }
    _currentTheme.value = _currentTheme.value.copy(theme = resolved)
  }

  override fun setColoredNaviBar(colored: Boolean) {
    storage.coloredNaviBar = colored
    _currentTheme.value = _currentTheme.value.copy(coloredNaviBar = colored)
  }

  override fun setPrimary(color: Color) {
    storage.primaryColor = color.toArgb()
    _currentTheme.value = _currentTheme.value.copy(primary = color)
  }

  override fun setSecondary(color: Color) {
    storage.secondaryColor = color.toArgb()
    _currentTheme.value = _currentTheme.value.copy(secondary = color)
  }

}
