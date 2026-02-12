package remix.myplayer.ui.screen.playing

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.ThemeUtil
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun PlayingContainer(content: @Composable () -> Unit) {
  val settingState by settingViewModel.settingsState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val theme = LocalTheme.current
  val initialColor = Color(
    ThemeUtil.resolveColor(
      context,
      R.attr.colorSurface,
      if (theme.isLight) Color.White.value.toInt() else Color.Black.value.toInt()
    )
  )

  if (!theme.isLight) {
    val background = theme.mainBackground
    val brush = Brush.verticalGradient(colors = listOf(background, background))
    Container(brush = brush, content = content)
    return
  }

  when (settingState.playingScreen.background) {
    SettingPrefs.BACKGROUND_ADAPTIVE_COLOR -> {
      val swatch by playbackViewModel.swatch.collectAsStateWithLifecycle()
      val color = remember(initialColor) { Animatable(initialValue = initialColor) }
      val brush = Brush.verticalGradient(colors = listOf(color.value, initialColor))
      Container(brush = brush, content = content)

      LaunchedEffect(swatch, initialColor) {
        color.animateTo(Color(swatch.rgb), animationSpec = tween(600))
      }
    }

    SettingPrefs.BACKGROUND_THEME -> {
      val brush = Brush.verticalGradient(colors = listOf(theme.primary, initialColor))
      Container(brush = brush, content = content)
    }

    else -> {
      Container(brush = null, content = content)
    }
  }
}

@Composable
private fun Container(
  brush: Brush?,
  content: @Composable () -> Unit
) {
  val baseModifier = Modifier
    .fillMaxSize()
    .navigationBarsPadding()
  val modifier = if (brush != null) {
    baseModifier.background(brush = brush, shape = RectangleShape)
  } else {
    baseModifier
  }

  Column(modifier = modifier) {
    Spacer(Modifier.statusBarsPadding())
    content()
  }
}
