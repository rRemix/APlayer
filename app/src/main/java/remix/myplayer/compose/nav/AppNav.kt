package remix.myplayer.compose.nav

import androidx.compose.animation.AnimatedContentScope
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.core.net.toUri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import remix.myplayer.BuildConfig
import remix.myplayer.compose.ui.dialog.DialogContainer
import remix.myplayer.compose.ui.screen.AboutScreen
import remix.myplayer.compose.ui.screen.HomeScreen
import remix.myplayer.compose.ui.screen.SongChooseScreen
import remix.myplayer.compose.ui.screen.playing.PlayingScreen
import remix.myplayer.compose.ui.screen.setting.SettingScreen

const val RouteHome = "home"
const val RouteSetting = "setting"
const val RouteSongChoose = "song_choose"
const val RoutePlayingScreen = "playing_screen"
const val RouteAbout = "about"

val playingScreenDeepLink = "aplayer://playingScreen".toUri()

@Composable
fun AppNav() {
  DialogContainer()

  NavHost(LocalNavController.current, startDestination = RouteHome) {
    normalAnimatedScreen(
      RouteHome,
    ) {
      HomeScreen()
    }

    normalAnimatedScreen(RouteSetting) {
      SettingScreen()
    }

    normalAnimatedScreen("${RouteSongChoose}/{id}/{name}", arguments = listOf(navArgument("id") {
      type = NavType.LongType
    })) {
      val id = it.arguments?.getLong("id") ?: return@normalAnimatedScreen
      val name = it.arguments?.getString("name") ?: return@normalAnimatedScreen
      SongChooseScreen(id, name)
    }

    normalAnimatedScreen(RouteAbout) {
      AboutScreen()
    }

    composable(
      RoutePlayingScreen,
      deepLinks = listOf(navDeepLink {
        uriPattern = playingScreenDeepLink.toString()
      }),
      // playingScreen has special animation
      enterTransition = {
        slideInFromBottom()
      },
      popExitTransition = {
        slideOutToBottom()

      }) {
      PlayingScreen()
    }
  }
}

private fun NavGraphBuilder.normalAnimatedScreen(
  route: String,
  arguments: List<NamedNavArgument> = emptyList(),
  deepLinks: List<NavDeepLink> = emptyList(),
  content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
  composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = {
      slideInFromRight()
    },
    exitTransition = {
      // playingScreen has special animation
      when (targetState.destination.route) {
        RoutePlayingScreen -> {
          slideOutToTop()
        }

        else -> {
          slideOutToLeft()
        }
      }
    },
    popEnterTransition = {
      // playingScreen has special animation
      when (initialState.destination.route) {
        RoutePlayingScreen -> {
          slideInFromTop()
        }

        else -> {
          slideInFromLeft()
        }
      }
    },
    popExitTransition = {
      slideOutToRight()
    },
    content = content
  )
}

private val duration = if (BuildConfig.DEBUG) 2000 else 350

//private val slideSpec = tween<IntOffset>(durationMillis = duration)
private val slideSpec = spring(
  stiffness = Spring.StiffnessMediumLow,
  visibilityThreshold = IntOffset.VisibilityThreshold
)

//private val fadeSpec = tween<Float>(durationMillis = duration)
private val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

private fun slideInFromRight(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    it
  }) + fadeIn(animationSpec = fadeSpec)

private fun slideInFromLeft(): EnterTransition =
  slideInHorizontally(animationSpec = slideSpec, initialOffsetX = {
    (-it * 0.35f).toInt()
  }) + fadeIn(animationSpec = fadeSpec)

private fun slideOutToRight(): ExitTransition =
  slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it }) + fadeOut(
    animationSpec = fadeSpec
  )

private fun slideOutToLeft(): ExitTransition? = slideOutHorizontally(
  animationSpec = slideSpec,
  targetOffsetX = { (-it * 0.35f).toInt() }) + fadeOut(animationSpec = fadeSpec)

private fun slideInFromTop(): EnterTransition = slideInVertically(
  animationSpec = slideSpec,
  initialOffsetY = { -(it * 0.2).toInt() }) +
    fadeIn(animationSpec = fadeSpec, initialAlpha = 0.5f)

private fun slideOutToTop(): ExitTransition = slideOutVertically(
  animationSpec = slideSpec,
  targetOffsetY = { -(it * 0.2).toInt() }) + fadeOut(animationSpec = fadeSpec)

private fun slideInFromBottom(): EnterTransition =
  slideInVertically(
    animationSpec = slideSpec,
    initialOffsetY = { (it * 0.05).toInt() }) +
      fadeIn(animationSpec = fadeSpec, initialAlpha = 0.5f)

private fun slideOutToBottom(): ExitTransition =
  slideOutVertically(
    animationSpec = slideSpec,
    targetOffsetY = { (it * 0.2).toInt() }) + fadeOut(animationSpec = fadeSpec)