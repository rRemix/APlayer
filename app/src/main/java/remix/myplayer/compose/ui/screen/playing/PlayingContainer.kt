package remix.myplayer.compose.ui.screen.playing

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
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.playingViewModel
import remix.myplayer.theme.Theme

@Composable
fun PlayingContainer(content: @Composable () -> Unit) {
  val playingVM = playingViewModel
  val theme = LocalTheme.current
  val context = LocalContext.current
  val swatch by playingVM.swatch.collectAsStateWithLifecycle()

  val initialColor = Color(
    Theme.resolveColor(
      context,
      R.attr.colorSurface,
      if (theme.isLight) Color.White.value.toInt() else Color.Black.value.toInt()
    )
  )
  val color = remember { Animatable(initialValue = initialColor) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .background(
        brush = Brush.verticalGradient(colors = listOf(color.value, initialColor)),
        shape = RectangleShape
      )
  ) {
    Spacer(Modifier.statusBarsPadding())
    content()
  }

  LaunchedEffect(swatch) {
    color.snapTo(initialColor)
    color.animateTo(Color(swatch.rgb), animationSpec = tween(1000))
  }
}