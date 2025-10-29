package remix.myplayer.ui.screen.playing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.glide.addBitmapListener
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.service.Command
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.playbackViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun PlayingCover(modifier: Modifier, song: Song) {
  val density = LocalDensity.current
  val isPortrait = LocalContext.current.isPortraitOrientation()

  val playbackVM = playbackViewModel

  val offsetBase = with(density) {
    val base = LocalConfiguration.current.screenWidthDp.dp.toPx()
    if (isPortrait) {
      base
    } else {
      base / 2
    }
  }

  val offsetAnimation = remember {
    Animatable(0f)
  }
  val scaleAnimation = remember {
    Animatable(1f)
  }

  GlideImage(
    model = song,
    contentDescription = "PlayingCover",
    failure = placeholder(LocalTheme.current.albumPlaceHolder),
    loading = placeholder(LocalTheme.current.albumPlaceHolder),
    modifier = Modifier
      .offset(with(density) { offsetAnimation.value.toDp() })
      .scale(scaleAnimation.value)
      .then(modifier)
  ) { builder ->
    builder.addBitmapListener { bitmap ->
      playbackVM.updateSwatch(bitmap)
    }
  }

  var first by remember { mutableStateOf(true) }
  LaunchedEffect(song) {
    if (!first) {
      val offset = offsetBase
      offsetAnimation.animateTo(
        targetValue = if (playbackVM.lastOp == Command.PREV) offset else -offset,
        animationSpec = spring<Float>(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMedium
        )
      )
      offsetAnimation.snapTo(0f)

      scaleAnimation.snapTo(0.85f)
      scaleAnimation.animateTo(
        targetValue = 1f,
        animationSpec = spring<Float>(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium * 3
        )
      )
    }
    first = false
  }
}