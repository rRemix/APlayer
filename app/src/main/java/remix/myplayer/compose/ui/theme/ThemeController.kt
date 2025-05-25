package remix.myplayer.compose.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import remix.myplayer.compose.prefs.Theme
import javax.inject.Inject

val LocalThemeController = compositionLocalOf<ThemeController> {
  error("no controller provide")
}

interface ThemeController {
  val appTheme: AppTheme

  fun setTheme(dark: String = AppTheme.FOLLOW_SYSTEM, black: Boolean = false)

  fun setPrimary(color: Int)

  fun setSecondary(color: Int)
}

class ThemeControllerImpl @Inject constructor(private val theme: Theme) : ThemeController {
  private val _currentTheme: MutableState<AppTheme> = mutableStateOf(AppTheme(
    primary = Color(theme.primaryColor),
    secondary = Color(theme.secondaryColor),
    theme = theme.theme()
  ))

  override val appTheme: AppTheme
    get() = _currentTheme.value

  override fun setTheme(dark: String, black: Boolean) {
    theme.darkTheme = dark
    theme.blackTheme = black
    _currentTheme.value = _currentTheme.value.copy(theme = theme.theme())
  }

  override fun setPrimary(color: Int) {
    theme.primaryColor = color
    _currentTheme.value = _currentTheme.value.copy(primary = Color(color))
  }

  override fun setSecondary(color: Int) {
    theme.secondaryColor = color
    _currentTheme.value = _currentTheme.value.copy(secondary = Color(color))
  }

}