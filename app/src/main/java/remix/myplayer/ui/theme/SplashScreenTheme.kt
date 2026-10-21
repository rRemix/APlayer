package remix.myplayer.ui.theme

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import remix.myplayer.R
import remix.myplayer.data.prefs.ThemePrefs

fun Activity.updateSplashScreenTheme(darkTheme: String) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
    return
  }

  // The system persists this theme for future launches, before our process starts.
  // Keep the theme resource names stable across app versions.
  val theme = when (darkTheme) {
    ThemePrefs.ALWAYS_OFF -> R.style.AppTheme_Starting_Light
    ThemePrefs.ALWAYS_ON -> R.style.AppTheme_Starting_Dark
    else -> Resources.ID_NULL
  }
  splashScreen.setSplashScreenTheme(theme)
}
