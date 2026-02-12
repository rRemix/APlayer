package remix.myplayer.ui.screen.playing

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt

private const val HORIZONTAL_FLIP_ENTER_DURATION_MS = 340
private const val HORIZONTAL_FLIP_EXIT_DURATION_MS = 260
private const val HORIZONTAL_FLIP_ENTER_ANGLE = 88f
private const val HORIZONTAL_FLIP_EXIT_ANGLE = 88f
private const val HORIZONTAL_FLIP_CAMERA_DISTANCE = 24f

private const val SLICE_STAGGER_ENTER_DURATION_MS = 420
private const val SLICE_STAGGER_EXIT_DURATION_MS = 360
private const val SLICE_STAGGER_COUNT = 7
private const val SLICE_STAGGER_STEP = 0.075f
private const val SLICE_STAGGER_OFFSET_FACTOR = 0.2f

private const val COVER_ANIMATION_MIN_DURATION_MS = 80

internal fun coverAnimationDuration(durationMs: Int, speed: Float): Int {
  return (durationMs / normalizeCoverAnimationSpeed(speed)).roundToInt()
    .coerceAtLeast(COVER_ANIMATION_MIN_DURATION_MS)
}

@Composable
internal fun AnimatedContentScope.horizontalFlip3DModifier(
  modifier: Modifier,
  isTargetContent: Boolean,
  isPrevious: Boolean,
  speed: Float = 1f
): Modifier {
  val enterAngle = if (isPrevious) -HORIZONTAL_FLIP_ENTER_ANGLE else HORIZONTAL_FLIP_ENTER_ANGLE
  val exitAngle = if (isPrevious) HORIZONTAL_FLIP_EXIT_ANGLE else -HORIZONTAL_FLIP_EXIT_ANGLE
  val enterDuration = coverAnimationDuration(HORIZONTAL_FLIP_ENTER_DURATION_MS, speed)
  val exitDuration = coverAnimationDuration(HORIZONTAL_FLIP_EXIT_DURATION_MS, speed)

  val rotationY by transition.animateFloat(
    transitionSpec = {
      if (targetState == EnterExitState.Visible) {
        tween(enterDuration)
      } else {
        tween(exitDuration)
      }
    },
    label = "CoverHorizontalFlipRotationY"
  ) { state ->
    when (state) {
      EnterExitState.PreEnter -> if (isTargetContent) enterAngle else 0f
      EnterExitState.Visible -> 0f
      EnterExitState.PostExit -> if (isTargetContent) 0f else exitAngle
    }
  }

  val contentOrigin = when {
    isTargetContent && isPrevious -> TransformOrigin(0f, 0.5f)
    isTargetContent -> TransformOrigin(1f, 0.5f)
    isPrevious -> TransformOrigin(1f, 0.5f)
    else -> TransformOrigin(0f, 0.5f)
  }

  return modifier.graphicsLayer {
    this.rotationY = rotationY
    transformOrigin = contentOrigin
    cameraDistance = HORIZONTAL_FLIP_CAMERA_DISTANCE * density
  }
}

@Composable
internal fun AnimatedContentScope.sliceStaggerModifier(
  modifier: Modifier,
  isTargetContent: Boolean,
  isPrevious: Boolean,
  speed: Float = 1f
): Modifier {
  val enterDuration = coverAnimationDuration(SLICE_STAGGER_ENTER_DURATION_MS, speed)
  val exitDuration = coverAnimationDuration(SLICE_STAGGER_EXIT_DURATION_MS, speed)
  val visibility by transition.animateFloat(
    transitionSpec = {
      if (targetState == EnterExitState.Visible) {
        tween(enterDuration)
      } else {
        tween(exitDuration)
      }
    },
    label = "CoverSliceStaggerProgress"
  ) { state ->
    when (state) {
      EnterExitState.PreEnter -> if (isTargetContent) 0f else 1f
      EnterExitState.Visible -> 1f
      EnterExitState.PostExit -> if (isTargetContent) 1f else 0f
    }
  }

  return modifier.drawWithContent {
    val sliceWidth = size.width / SLICE_STAGGER_COUNT
    val staggerSpan = SLICE_STAGGER_STEP * (SLICE_STAGGER_COUNT - 1)
    val normalization = (1f - staggerSpan).coerceAtLeast(0.2f)
    val exitPhase = 1f - visibility
    val direction = if (isPrevious) -1f else 1f

    for (slice in 0 until SLICE_STAGGER_COUNT) {
      val order = if (isPrevious) SLICE_STAGGER_COUNT - 1 - slice else slice
      val visible = if (isTargetContent) {
        ((visibility - order * SLICE_STAGGER_STEP) / normalization).coerceIn(0f, 1f)
      } else {
        1f - ((exitPhase - order * SLICE_STAGGER_STEP) / normalization).coerceIn(0f, 1f)
      }
      if (visible <= 0f) {
        continue
      }

      val oscillation = if (slice % 2 == 0) 1f else -1f
      val offsetX = (1f - visible) * size.width * SLICE_STAGGER_OFFSET_FACTOR * direction * oscillation
      val left = sliceWidth * slice
      val right = if (slice == SLICE_STAGGER_COUNT - 1) size.width else left + sliceWidth

      clipRect(
        left = left,
        top = 0f,
        right = right,
        bottom = size.height
      ) {
        withTransform({
          translate(left = offsetX)
        }) {
          this@drawWithContent.drawContent()
        }
      }
    }
  }
}
