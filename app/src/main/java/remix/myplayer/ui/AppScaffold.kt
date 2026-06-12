package remix.myplayer.ui

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import remix.myplayer.ui.screen.playing.PlayingPanel
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.PlayingScreenValue
import remix.myplayer.viewmodel.mainViewModel
import kotlin.math.roundToInt

@Composable
fun AppScaffold(content: @Composable () -> Unit) {
  val mainVM = mainViewModel
  val scope = rememberCoroutineScope()
  var screenHeight by remember { mutableFloatStateOf(10000f) }
  val playingScreenState = mainVM.playingScreenState
  var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
  val isVisible by remember {
    derivedStateOf {
      playingScreenState.progress(PlayingScreenValue.Hidden, PlayingScreenValue.Expanded) > 0.01f
    }
  }

  fun calculateRenderedOffset(): Float {
    val currentOffset = playingScreenState.offset.takeIf { !it.isNaN() } ?: screenHeight
    return currentOffset + (screenHeight - currentOffset) * predictiveBackProgress
  }

  fun calculateRenderedProgress(renderedOffset: Float = calculateRenderedOffset()): Float {
    return (1f - renderedOffset / screenHeight).coerceIn(0f, 1f)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(LocalTheme.current.mainBackground)
      .onSizeChanged { size ->
        val newHeight = size.height.toFloat()
        if (screenHeight != newHeight) {
          screenHeight = newHeight
          playingScreenState.updateAnchors(
            DraggableAnchors {
              PlayingScreenValue.Hidden at newHeight
              PlayingScreenValue.Expanded at 0f
            },
            newTarget = playingScreenState.currentValue
          )
        }
      }
  ) {
    Box(
      Modifier
        .fillMaxSize()
        .graphicsLayer {
          val renderedProgress = calculateRenderedProgress()
          translationY = -size.height * 0.2f * renderedProgress
          alpha = 1f - 0.5f * renderedProgress
        }) {
      content()
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .offset {
          val renderedOffset = calculateRenderedOffset()
          IntOffset(0, renderedOffset.roundToInt())
        }
        .anchoredDraggable(
          state = playingScreenState,
          orientation = Orientation.Vertical,
          enabled = isVisible,
          flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = playingScreenState,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioLowBouncy,
              stiffness = Spring.StiffnessMediumLow
            )
          )
        )
        .graphicsLayer {
          val renderedProgress = calculateRenderedProgress()
          alpha = FastOutSlowInEasing.transform(renderedProgress)
          clip = renderedProgress > 0f
        }
    ) {
      PredictiveBackHandler(
        enabled = isVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
      ) { progress ->
        try {
          progress.collect { backEvent: BackEventCompat ->
            predictiveBackProgress = backEvent.progress
          }
          playingScreenState.animateTo(PlayingScreenValue.Hidden)
          predictiveBackProgress = 0f
        } catch (_: CancellationException) {
          predictiveBackProgress = 0f
        }
      }

      BackHandler(enabled = isVisible && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        scope.launch {
          playingScreenState.animateTo(PlayingScreenValue.Hidden)
        }
      }

      PlayingPanel(isVisible)
    }
  }
}
