package remix.myplayer.compose.ui.screen.playing

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.compose.viewmodel.PlayingViewModel
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.service.Command

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun PlayingCover(modifier: Modifier, song: Song) {
  val density = LocalDensity.current
  val isPortrait = LocalContext.current.isPortraitOrientation()

  val playingVM = activityViewModel<PlayingViewModel>()
  val musicVM = activityViewModel<MusicViewModel>()

  val offsetBase = with(density) {
    val base = LocalConfiguration.current.screenWidthDp.dp.toPx()
    if (isPortrait) {
      base
    } else {
      base /  2
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
  ) {
    it.addListener(object : RequestListener<Drawable> {
      override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable?>,
        isFirstResource: Boolean
      ): Boolean {
        playingVM.updateSwatch(null)
        return false
      }

      override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable?>?,
        dataSource: DataSource,
        isFirstResource: Boolean
      ): Boolean {
        playingVM.updateSwatch(if (resource is BitmapDrawable) resource.bitmap else null)
        return false
      }
    })
  }

  var first by remember { mutableStateOf(true) }
  LaunchedEffect(song) {
    if (!first) {
      val offset = offsetBase
      offsetAnimation.animateTo(
        targetValue = if (musicVM.lastOp == Command.PREV) offset else -offset,
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