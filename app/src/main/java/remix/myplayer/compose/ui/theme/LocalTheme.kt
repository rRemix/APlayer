package remix.myplayer.compose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.core.graphics.toColorInt
import remix.myplayer.R
import remix.myplayer.util.ColorUtil


val LocalTheme = compositionLocalOf<AppTheme> {
  return@compositionLocalOf AppTheme(
    primary = Color(0xff698cf6),
    secondary = Color(0xff698cf6),
    theme = AppTheme.LIGHT
  )
}

data class AppTheme(
  var primary: Color,
  var secondary: Color,
  var theme: String
) {

  val isLight: Boolean
    get() = theme == LIGHT
  val isDark: Boolean
    get() = theme == DARK
  val isBlack: Boolean
    get() = theme == BLACK

  val textPrimary
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (isLight) R.color.light_text_color_primary else R.color.dark_text_color_primary)

  val textSecondary
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (isLight) R.color.light_text_color_secondary else R.color.dark_text_color_secondary)

  val ripple
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (isLight) R.color.light_ripple_color else R.color.dark_ripple_color)

  val tabText
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (isPrimaryCloseToWhite) R.color.dark_normal_tab_text_color else R.color.light_normal_tab_text_color)

  val mainBackground
    @Composable
    @ReadOnlyComposable
    get() = colorResource(
      when (theme) {
        LIGHT -> R.color.light_background_color_main
        DARK -> R.color.dark_background_color_main
        BLACK -> R.color.black_background_color_main
        else -> throw IllegalArgumentException("unknown theme: $theme")
      }
    )

  val dialogBackground
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (isLight) R.color.light_background_color_dialog else R.color.dark_background_color_dialog)

  val libraryBackground
    @Composable
    @ReadOnlyComposable
    get() = colorResource(
      when (theme) {
        LIGHT -> R.color.light_library_color
        DARK -> R.color.dark_library_color
        BLACK -> R.color.black_library_color
        else -> throw IllegalArgumentException("unknown theme: $theme")
      }
    )

  val libraryButton
    @Composable
    @ReadOnlyComposable
    get() = Color((if (isLight) "#6C6A6C" else "#FFFFFF").toColorInt())

  val bottomBarButton
    @Composable
    @ReadOnlyComposable
    get() = Color((if (isLight) "#323334" else "#FFFFFF").toColorInt())

  val drawerEffect
    @Composable
    @ReadOnlyComposable
    get() = colorResource(
      when (theme) {
        LIGHT -> R.color.drawer_effect_light
        DARK -> R.color.drawer_effect_dark
        BLACK -> R.color.drawer_effect_light
        else -> throw IllegalArgumentException("unknown theme: $theme")
      }
    )

  val drawerDefault
    @Composable
    @ReadOnlyComposable
    get() = colorResource(
      when (theme) {
        LIGHT -> R.color.drawer_default_light
        DARK -> R.color.drawer_default_dark
        BLACK -> R.color.drawer_default_black
        else -> throw IllegalArgumentException("unknown theme: $theme")
      }
    )

  val iconColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLight) Color.Black else Color.White

  val primaryReverse: Color
    @Composable
    @ReadOnlyComposable
    get() = if (!isPrimaryCloseToWhite) Color.White else Color.Black

  val textPrimaryReverse
    @Composable
    @ReadOnlyComposable
    get() = colorResource(if (!isPrimaryCloseToWhite) R.color.dark_text_color_primary else R.color.light_text_color_primary)

  val highLightText: Color
    @Composable
    @ReadOnlyComposable
    get() {
      var primaryColor = primary
      if (isPrimaryCloseToWhite && isLight) {
        primaryColor = textPrimary
      }
      if (isPrimaryCloseToBlack && isBlack) {
        primaryColor = textPrimary
      }
      return primaryColor
    }

  val albumPlaceHolder: Int
    @Composable
    @ReadOnlyComposable
    get() = if (isLight) R.drawable.album_empty_bg_day else R.drawable.album_empty_bg_night

  val artistPlaceHolder: Int
    @Composable
    @ReadOnlyComposable
    get() = if (isLight) R.drawable.artist_empty_bg_day else R.drawable.artist_empty_bg_night

  val isPrimaryLight: Boolean
    get() = ColorUtil.isColorLight(primary.toArgb())

  val isPrimaryCloseToWhite: Boolean
    get() = ColorUtil.isColorCloseToWhite(primary.toArgb())

  val isPrimaryCloseToBlack: Boolean
    get() = ColorUtil.isColorCloseToBlack(primary.toArgb())


  companion object {
    const val LIGHT = "Light"
    const val DARK = "Dark"
    const val BLACK = "Black"

    const val ALWAYS_OFF = "always_off"
    const val ALWAYS_ON = "always_on"
    const val FOLLOW_SYSTEM = "follow_system"
    var sColoredNavigation: Boolean = false
    var sImmersiveMode: Boolean = false

    fun darkenColor(color: Color): Color {
      return Color(ColorUtil.darkenColor(color.toArgb()))
    }
  }
}