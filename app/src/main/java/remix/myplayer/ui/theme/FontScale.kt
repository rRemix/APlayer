package remix.myplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import remix.myplayer.data.prefs.SettingPrefs

@Composable
fun ProvideAppFontScale(
  fontScale: Float,
  content: @Composable () -> Unit
) {
  val parentDensity = LocalDensity.current
  val userScale = SettingPrefs.normalizeUiFontScale(fontScale)

  CompositionLocalProvider(
    LocalDensity provides Density(
      density = parentDensity.density,
      fontScale = parentDensity.fontScale * userScale
    )
  ) {
    content()
  }
}
