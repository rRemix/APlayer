package remix.myplayer.data.prefs

import android.content.Context
import android.content.res.Configuration
import androidx.core.graphics.toColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePrefs @Inject constructor(
  @param:ApplicationContext val context: Context,
  settingPrefs: SettingPrefs
) :
  AbstractPref(context, name = PrefKeys.Theme.NAME) {

  var primaryColor by PrefsDelegate(sp, PrefKeys.Theme.PRIMARY_COLOR, "#698cf6".toColorInt())
  var secondaryColor by PrefsDelegate(sp, PrefKeys.Theme.SECONDARY_COLOR, "#698cf6".toColorInt())

  // 从settingPrefs迁移
  var darkTheme by PrefsDelegate(
    sp,
    PrefKeys.Theme.DARK_THEME,
    settingPrefs.sp.getString(PrefKeys.Theme.DARK_THEME, FOLLOW_SYSTEM)!!
  )
  var blackTheme by PrefsDelegate(
    sp,
    PrefKeys.Theme.BLACK_THEME,
    settingPrefs.sp.getBoolean(PrefKeys.Theme.BLACK_THEME, false)
  )

  var coloredNaviBar by PrefsDelegate(sp, PrefKeys.Theme.COLOR_NAVIGATION, false)

  fun resolveTheme(
    darkTheme: String,
    blackTheme: Boolean,
    uiMode: Int = context.resources.configuration.uiMode
  ): String {
    return if (darkTheme == ALWAYS_ON || (darkTheme == FOLLOW_SYSTEM && (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
      if (blackTheme) {
        BLACK
      } else {
        DARK
      }
    } else {
      LIGHT
    }
  }

  companion object {

    const val LIGHT = "light"
    const val DARK = "dark"
    const val BLACK = "black"

    const val ALWAYS_OFF = "always_off"
    const val ALWAYS_ON = "always_on"
    const val FOLLOW_SYSTEM = "follow_system"

  }
}
