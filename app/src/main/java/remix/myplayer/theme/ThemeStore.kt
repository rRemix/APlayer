package remix.myplayer.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.SPUtil

object ThemeStore {
  private const val KEY_NAME = "aplayer-theme"
  private const val LIGHT = "light"
  private const val DARK = "dark"
  private const val BLACK = "black"
  const val ALWAYS_OFF = "always_off"
  const val ALWAYS_ON = "always_on"
  const val FOLLOW_SYSTEM = "follow_system"
  private const val KEY_PRIMARY_COLOR = "primary_color"
  private const val KEY_ACCENT_COLOR = "accent_color"
  const val STATUS_BAR_ALPHA = 150

  @JvmField
  var sColoredNavigation: Boolean = false

  @JvmField
  var sImmersiveMode: Boolean = false

  val theme: String
    get() {
      val darkTheme = SPUtil.getValue(
        App.context,
        SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.DARK_THEME,
        FOLLOW_SYSTEM
      )
      return if (darkTheme == ALWAYS_ON || (darkTheme == FOLLOW_SYSTEM && (App.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
        if (SPUtil.getValue(
            App.context,
            SPUtil.SETTING_KEY.NAME,
            SPUtil.SETTING_KEY.BLACK_THEME,
            false
          )
        ) {
          BLACK
        } else {
          DARK
        }
      } else {
        LIGHT
      }
    }

  @JvmStatic
  @get:StyleRes
  val themeRes: Int
    get() = when (theme) {
      LIGHT -> R.style.Theme_APlayer
      BLACK -> R.style.Theme_APlayer_Black
      DARK -> R.style.Theme_APlayer_Dark
      else -> R.style.Theme_APlayer
    }

  @JvmStatic
  @get:ColorInt
  val highLightTextColor: Int
    get() {
      var primaryColor = materialPrimaryColor
      if (ColorUtil.isColorCloseToWhite(primaryColor) && isLightTheme) {
        primaryColor = textColorPrimary
      }
      if (ColorUtil.isColorCloseToBlack(primaryColor) && isBlackTheme) {
        primaryColor = textColorPrimary
      }
      return primaryColor
    }

  @JvmStatic
  @get:ColorInt
  var materialPrimaryColor: Int
    get() = SPUtil.getValue(
      App.context, KEY_NAME, KEY_PRIMARY_COLOR, Color.parseColor("#698cf6")
    )
    set(@ColorInt value) {
      SPUtil.putValue(App.context, KEY_NAME, KEY_PRIMARY_COLOR, value)
    }

  @get:ColorInt
  val materialPrimaryDarkColor: Int
    get() = ColorUtil.darkenColor(materialPrimaryColor)

  @JvmStatic
  @get:ColorInt
  var accentColor: Int
    get() {
      var accentColor = SPUtil.getValue(
        App.context, KEY_NAME, KEY_ACCENT_COLOR, Color.parseColor("#698cf6")
      )
      if (ColorUtil.isColorCloseToWhite(accentColor)) {
        accentColor = ColorUtil.getColor(R.color.accent_gray_color)
      }
      return accentColor
    }
    set(value) {
      SPUtil.putValue(App.context, KEY_NAME, KEY_ACCENT_COLOR, value)
    }

  @JvmStatic
  @get:ColorInt
  val navigationBarColor: Int
    get() = materialPrimaryColor

  @JvmStatic
  @get:ColorInt
  val statusBarColor: Int
    get() = if (sImmersiveMode) {
      materialPrimaryColor
    } else {
      materialPrimaryDarkColor
    }

  @JvmStatic
  @get:ColorInt
  val textColorPrimary: Int
    get() = ColorUtil.getColor(
      if (isLightTheme) {
        R.color.light_text_color_primary
      } else {
        R.color.dark_text_color_primary
      }
    )

  @get:ColorInt
  val materialPrimaryColorReverse: Int
    get() = ColorUtil.getColor(
      if (!isMDColorCloseToWhite) {
        R.color.white
      } else {
        R.color.black
      }
    )

  @JvmStatic
  @get:ColorInt
  val textColorPrimaryReverse: Int
    get() = ColorUtil.getColor(
      if (!isMDColorCloseToWhite) {
        R.color.dark_text_color_primary
      } else {
        R.color.light_text_color_primary
      }
    )

  @get:ColorInt
  val textColorSecondary: Int
    get() = ColorUtil.getColor(
      if (isLightTheme) {
        R.color.light_text_color_secondary
      } else {
        R.color.dark_text_color_secondary
      }
    )

  @JvmStatic
  @ColorInt
  fun getBackgroundColorMain(context: Context?): Int {
    return Theme.resolveColor(context, R.attr.background_color_main)
  }

  @JvmStatic
  @ColorInt
  fun getBackgroundColorDialog(context: Context?): Int {
    return Theme.resolveColor(context, R.attr.background_color_dialog)
  }

  @get:ColorInt
  val rippleColor: Int
    get() = ColorUtil.getColor(
      if (isLightTheme) {
        R.color.light_ripple_color
      } else {
        R.color.dark_ripple_color
      }
    )

  @get:ColorInt
  val playerBtnColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#6c6a6c"
      } else {
        "#6b6b6b"
      }
    )

  @get:ColorInt
  val playerTitleColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#333333"
      } else {
        "#e5e5e5"
      }
    )

  @get:ColorInt
  val playerProgressColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#efeeed"
      } else {
        "#343438"
      }
    )

  @JvmStatic
  @get:ColorInt
  val bottomBarBtnColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#323334"
      } else {
        "#ffffff"
      }
    )

  @JvmStatic
  @get:ColorInt
  val libraryBtnColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#6c6a6c"
      } else {
        "#ffffff"
      }
    )

  @get:ColorInt
  val playerNextSongBgColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#fafafa"
      } else {
        "#343438"
      }
    )

  @get:ColorInt
  val playerNextSongTextColor: Int
    get() = Color.parseColor(
      if (isLightTheme) {
        "#a8a8a8"
      } else {
        "#e5e5e5"
      }
    )

  @JvmStatic
  @get:ColorInt
  val drawerEffectColor: Int
    get() = ColorUtil.getColor(
      when (theme) {
        LIGHT -> R.color.drawer_effect_light
        DARK -> R.color.drawer_effect_dark
        BLACK -> R.color.drawer_effect_black
        else -> R.color.drawer_effect_light
      }
    )

  @JvmStatic
  @get:ColorInt
  val drawerDefaultColor: Int
    get() = ColorUtil.getColor(
      when (theme) {
        LIGHT -> R.color.drawer_default_light
        DARK -> R.color.drawer_default_dark
        BLACK -> R.color.drawer_default_black
        else -> R.color.drawer_default_light
      }
    )

  @get:ColorInt
  val colorOnPrimary: Int
    get() {
      return ColorUtil.getColor(
        if (isMDColorLight) {
        R.color.design_dark_default_color_on_primary
      } else {
        R.color.design_default_color_on_primary
      })
    }

  @JvmStatic
  val mDDialogTheme: com.afollestad.materialdialogs.Theme
    get() = if (isMDColorLight) {
      com.afollestad.materialdialogs.Theme.LIGHT
    } else {
      com.afollestad.materialdialogs.Theme.DARK
    }

  @JvmStatic
  val isMDColorLight: Boolean
    get() = ColorUtil.isColorLight(materialPrimaryColor)

  val isMDColorCloseToWhite: Boolean
    get() = ColorUtil.isColorCloseToWhite(materialPrimaryColor)

  @JvmStatic
  val isLightTheme: Boolean
    get() = themeRes == R.style.Theme_APlayer

  private val isBlackTheme: Boolean
    get() = themeRes == R.style.Theme_APlayer_Black
}