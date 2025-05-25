package remix.myplayer.compose.prefs

import android.content.Context
import android.content.res.Configuration
import androidx.core.graphics.toColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.compose.ui.theme.AppTheme.Companion.ALWAYS_ON
import remix.myplayer.compose.ui.theme.AppTheme.Companion.BLACK
import remix.myplayer.compose.ui.theme.AppTheme.Companion.DARK
import remix.myplayer.compose.ui.theme.AppTheme.Companion.FOLLOW_SYSTEM
import remix.myplayer.compose.ui.theme.AppTheme.Companion.LIGHT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Theme @Inject constructor(@ApplicationContext val context: Context) : AbstractPref(context, name = "aplayer-theme") {
  var primaryColor by PrefsDelegate(sp, "primary_color", "#698cf6".toColorInt())
  var secondaryColor by PrefsDelegate(sp, "accent_color", "#698cf6".toColorInt())

  var darkTheme by PrefsDelegate(sp, "dark_theme", FOLLOW_SYSTEM)
  var blackTheme by PrefsDelegate(sp, "black_theme", false)

  fun theme(): String {
    val darkTheme = darkTheme
    return if (darkTheme == ALWAYS_ON || (darkTheme == FOLLOW_SYSTEM && (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
      if (blackTheme) {
        BLACK
      } else {
        DARK
      }
    } else {
      LIGHT
    }
  }
}