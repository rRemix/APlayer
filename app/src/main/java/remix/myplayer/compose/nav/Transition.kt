package remix.myplayer.compose.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import remix.myplayer.BuildConfig


fun enterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
  {
    slideInFromRight()
  }

fun exitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
  // playingScreen has special animation
  when (targetState.destination.route) {
    RoutePlayingScreen -> {
      slideOutToTop()
    }

    else -> {
      slideOutToLeft()
    }
  }
}

fun popEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
  {
    // playingScreen has special animation
    when (initialState.destination.route) {
      RoutePlayingScreen -> {
        slideInFromTop()
      }

      else -> {
        slideInFromLeft()
      }
    }
  }

fun popExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards ExitTransition? =
  {
    slideOutToRight()
  }

private val duration = if (BuildConfig.DEBUG) 2000 else 350

//private val slideSpec = tween<IntOffset>(durationMillis = duration)
private val slideSpec = spring(
  stiffness = Spring.StiffnessMediumLow,
  visibilityThreshold = IntOffset.VisibilityThreshold
)

//private val fadeSpec = tween<Float>(durationMillis = duration)
private val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

fun slideInFromRight(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    it
  }) + fadeIn(animationSpec = fadeSpec)

fun slideInFromLeft(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    (-it * 0.35f).toInt()
  }) + fadeIn(animationSpec = fadeSpec)

fun slideOutToRight(): ExitTransition =
  slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it }) + fadeOut(
    animationSpec = fadeSpec
  )

fun slideOutToLeft(): ExitTransition? = slideOutHorizontally(
  animationSpec = slideSpec,
  targetOffsetX = { (-it * 0.35f).toInt() }) + fadeOut(animationSpec = fadeSpec)

fun slideInFromTop(): EnterTransition = slideInVertically(
  animationSpec = slideSpec,
  initialOffsetY = { -(it * 0.2).toInt() }) +
    fadeIn(animationSpec = fadeSpec, initialAlpha = 0.5f)

fun slideOutToTop(): ExitTransition = slideOutVertically(
  animationSpec = slideSpec,
  targetOffsetY = { -(it * 0.2).toInt() }) + fadeOut(animationSpec = fadeSpec)

fun slideInFromBottom(): EnterTransition =
  slideInVertically(
    animationSpec = slideSpec,
    initialOffsetY = { (it * 0.05).toInt() }) +
      fadeIn(animationSpec = fadeSpec, initialAlpha = 0.5f)

fun slideOutToBottom(): ExitTransition =
  slideOutVertically(
    animationSpec = slideSpec,
    targetOffsetY = { (it * 0.2).toInt() }) + fadeOut(animationSpec = fadeSpec)