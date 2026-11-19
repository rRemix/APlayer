package remix.myplayer.ui.nav

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

fun enterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
  {
    if (targetState.destination.route == RoutePlaying) {
      slideInVertically(animationSpec = slideSpec, initialOffsetY = { it }) +
          fadeIn(animationSpec = fadeSpec)
    } else {
      slideInFromRight()
    }
  }

fun exitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
  if (targetState.destination.route == RoutePlaying) {
    slideOutVertically(animationSpec = slideSpec, targetOffsetY = { (-it * 0.2f).toInt() }) +
        fadeOut(animationSpec = fadeSpec, targetAlpha = 0.5f)
  } else {
    slideOutToLeft()
  }
}

fun popEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
  {
    if (initialState.destination.route == RoutePlaying) slideInFromTop() else slideInFromLeft()
  }

fun popExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
  {
    if (initialState.destination.route == RoutePlaying) {
      slideOutVertically(animationSpec = slideSpec, targetOffsetY = { it }) +
          fadeOut(animationSpec = fadeSpec)
    } else {
      slideOutToRight()
    }
  }

private val slideSpec = spring(
  stiffness = Spring.StiffnessMediumLow,
  visibilityThreshold = IntOffset.VisibilityThreshold
)

private val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

fun slideInFromRight(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    it
  })

fun slideInFromLeft(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    (-it * 0.35f).toInt()
  })

fun slideOutToRight(): ExitTransition =
  slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it })

fun slideOutToLeft(): ExitTransition = slideOutHorizontally(
  animationSpec = slideSpec,
  targetOffsetX = { (-it * 0.35f).toInt() })

fun slideInFromTop(): EnterTransition = slideInVertically(
  animationSpec = slideSpec,
  initialOffsetY = { -(it * 0.2).toInt() }) +
    fadeIn(animationSpec = fadeSpec, initialAlpha = 0.5f)
