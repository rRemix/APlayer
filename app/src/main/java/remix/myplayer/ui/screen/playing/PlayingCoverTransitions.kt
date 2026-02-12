package remix.myplayer.ui.screen.playing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.roundToInt

private object CoverAnimationSpec {

  const val noBouncyDamping = Spring.DampingRatioNoBouncy
  const val mediumBouncyDamping = Spring.DampingRatioMediumBouncy

  const val enterStiffness = Spring.StiffnessLow
  const val exitStiffness = Spring.StiffnessMediumLow

}

internal const val COVER_ANIMATION_SPEED_MIN = 0.5f
internal const val COVER_ANIMATION_SPEED_MAX = 2f
internal const val COVER_ANIMATION_SPEED_STEP = 0.1f

internal fun normalizeCoverAnimationSpeed(speed: Float): Float {
  return speed.coerceIn(COVER_ANIMATION_SPEED_MIN, COVER_ANIMATION_SPEED_MAX)
}

internal fun snapCoverAnimationSpeed(speed: Float): Float {
  val normalized = normalizeCoverAnimationSpeed(speed)
  val snappedStep =
    ((normalized - COVER_ANIMATION_SPEED_MIN) / COVER_ANIMATION_SPEED_STEP).roundToInt()
  val snappedValue = COVER_ANIMATION_SPEED_MIN + snappedStep * COVER_ANIMATION_SPEED_STEP
  return ((snappedValue * 10f).roundToInt() / 10f).coerceIn(
    COVER_ANIMATION_SPEED_MIN,
    COVER_ANIMATION_SPEED_MAX
  )
}

private fun coverAnimationStiffness(stiffness: Float, speed: Float): Float {
  return stiffness * normalizeCoverAnimationSpeed(speed)
}

private fun enterCoverAnimationStiffness(speed: Float): Float {
  return coverAnimationStiffness(CoverAnimationSpec.enterStiffness, speed)
}

private fun exitCoverAnimationStiffness(speed: Float): Float {
  return coverAnimationStiffness(CoverAnimationSpec.exitStiffness, speed)
}

internal fun <T> AnimatedContentTransitionScope<T>.buildCoverContentTransform(
  style: PlayingCoverAnimationStyle,
  isPrevious: Boolean,
  speed: Float = 1f
): ContentTransform {
  val transform = when (style) {
    PlayingCoverAnimationStyle.CLASSIC ->
      classicCoverTransform(isPrevious, speed) using SizeTransform(clip = false)
    PlayingCoverAnimationStyle.CARD_SQUEEZE -> cardSqueezeCoverTransform(isPrevious, speed)
    PlayingCoverAnimationStyle.PAGE_TURN -> horizontalFlipCoverTransform(speed)
    PlayingCoverAnimationStyle.SLICE_STAGGER -> sliceStaggerCoverTransform(speed)
    PlayingCoverAnimationStyle.DISSOLVE_ZOOM -> dissolveZoomCoverTransform(isPrevious, speed)
    PlayingCoverAnimationStyle.PARALLAX_PUSH -> parallaxPushCoverTransform(isPrevious, speed)
  }
  return transform
}

private fun classicCoverTransform(isPrevious: Boolean, speed: Float): ContentTransform {
  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  val enterSlide = slideInHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) { width -> directionAwareOffset(width, isPrevious, -1f, 1f) }

  val exitSlide = slideOutHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  ) { width -> directionAwareOffset(width, isPrevious, 1f, -1f) }

  val enterScale = scaleIn(
    initialScale = 0.85f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.mediumBouncyDamping,
      stiffness = enterStiffness
    )
  )
  val exitScale = scaleOut(
    targetScale = 0.85f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  )

  return (enterSlide + fadeIn(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) + enterScale).togetherWith(
    exitSlide + fadeOut(
      animationSpec = spring(
        dampingRatio = CoverAnimationSpec.noBouncyDamping,
        stiffness = exitStiffness
      )
    ) + exitScale
  )
}

private fun cardSqueezeCoverTransform(isPrevious: Boolean, speed: Float): ContentTransform {
  val enterOrigin = if (isPrevious) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
  val exitOrigin = if (isPrevious) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  val enterSlide = slideInHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) { width ->
    directionAwareOffset(width, isPrevious, -0.2f, 0.2f)
  }
  val exitSlide = slideOutHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  ) { width ->
    directionAwareOffset(width, isPrevious, 0.2f, -0.2f)
  }

  val enterSqueeze = scaleIn(
    initialScale = 0.06f,
    transformOrigin = enterOrigin,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  )
  val exitSqueeze = scaleOut(
    targetScale = 0.06f,
    transformOrigin = exitOrigin,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  )

  return (enterSlide + fadeIn(
    initialAlpha = 0.35f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) + enterSqueeze)
    .togetherWith(
      exitSlide + fadeOut(
        targetAlpha = 0.35f,
        animationSpec = spring(
          dampingRatio = CoverAnimationSpec.noBouncyDamping,
          stiffness = exitStiffness
        )
      ) + exitSqueeze
    )
    .apply { targetContentZIndex = 1f }
}

private fun horizontalFlipCoverTransform(
  speed: Float
): ContentTransform {
  // The actual 3D flip effect is handled by a custom Modifier (horizontalFlip3DModifier)
  // This transform only handles the cross-fade.

  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  return fadeIn(
    initialAlpha = 0.08f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  )
    .togetherWith(
      fadeOut(
        targetAlpha = 0f,
        animationSpec = spring(
          dampingRatio = CoverAnimationSpec.noBouncyDamping,
          stiffness = exitStiffness
        )
      )
    )
    .apply { targetContentZIndex = 1f }
}

private fun sliceStaggerCoverTransform(speed: Float): ContentTransform {
  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  return fadeIn(
    initialAlpha = 0.18f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  )
    .togetherWith(
      fadeOut(
        targetAlpha = 0f,
        animationSpec = spring(
          dampingRatio = CoverAnimationSpec.noBouncyDamping,
          stiffness = exitStiffness
        )
      )
    )
    .apply { targetContentZIndex = 1f }
}

private fun dissolveZoomCoverTransform(isPrevious: Boolean, speed: Float): ContentTransform {
  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  val enterSlide = slideInHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) { width ->
    directionAwareOffset(width, isPrevious, -0.08f, 0.08f)
  }
  val exitSlide = slideOutHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  ) { width ->
    directionAwareOffset(width, isPrevious, 0.08f, -0.08f)
  }

  val exitScale = scaleOut(
    targetScale = 0.94f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  )

  return (enterSlide + fadeIn(
    initialAlpha = 0.25f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ))
    .togetherWith(
      exitSlide + fadeOut(
        targetAlpha = 0.2f,
        animationSpec = spring(
          dampingRatio = CoverAnimationSpec.noBouncyDamping,
          stiffness = exitStiffness
        )
      ) + exitScale
    )
}

private fun parallaxPushCoverTransform(isPrevious: Boolean, speed: Float): ContentTransform {
  val enterStiffness = enterCoverAnimationStiffness(speed)
  val exitStiffness = exitCoverAnimationStiffness(speed)

  val enterSlide = slideInHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ) { width -> directionAwareOffset(width, isPrevious, -1f, 1f) }

  val exitSlide = slideOutHorizontally(
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  ) { width -> directionAwareOffset(width, isPrevious, 0.35f, -0.35f) }

//  val enterScale = scaleIn(
//    initialScale = 1.03f,
//    animationSpec = spring(
//      dampingRatio = CoverAnimationSpec.noBouncyDamping,
//      stiffness = enterStiffness
//    )
//  )
  val exitScale = scaleOut(
    targetScale = 0.96f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = exitStiffness
    )
  )

  return (enterSlide + fadeIn(
    initialAlpha = 0.7f,
    animationSpec = spring(
      dampingRatio = CoverAnimationSpec.noBouncyDamping,
      stiffness = enterStiffness
    )
  ))
    .togetherWith(
      exitSlide + fadeOut(
        targetAlpha = 0.5f,
        animationSpec = spring(
          dampingRatio = CoverAnimationSpec.noBouncyDamping,
          stiffness = exitStiffness
        )
      ) + exitScale
    )
    .apply { targetContentZIndex = 1f }
}

private fun directionAwareOffset(
  width: Int,
  isPrevious: Boolean,
  previousMultiplier: Float,
  nextMultiplier: Float
): Int {
  val factor = if (isPrevious) previousMultiplier else nextMultiplier
  return (width * factor).toInt()
}
