package remix.myplayer.ui.screen.playing

import androidx.compose.foundation.gestures.snapTo
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import remix.myplayer.ui.nav.DetailScreenRoute
import remix.myplayer.ui.nav.ExtraHidePlayingScreenOnEntry
import remix.myplayer.ui.nav.ExtraRestorePlayingScreen
import remix.myplayer.ui.nav.isSameDetail
import remix.myplayer.viewmodel.MainViewModel
import remix.myplayer.viewmodel.PlayingScreenValue

/** Keep the player covering the previous screen until the detail destination is ready. */
internal suspend fun NavHostController.openPlayingDetail(
  route: DetailScreenRoute,
  mainVM: MainViewModel,
) {
  val current = currentBackStackEntry ?: return
  // Navigation immediately changes the current entry; ignore taps during its transition.
  if (current.lifecycle.currentState != Lifecycle.State.RESUMED) return

  if (current.isSameDetail(route)) {
    mainVM.playingScreenState.snapTo(PlayingScreenValue.Hidden)
    return
  }

  navigate(route)
  val entry = currentBackStackEntry ?: return
  entry.savedStateHandle[ExtraRestorePlayingScreen] = true
  // Let the destination finish the handoff, including after a configuration change.
  entry.savedStateHandle[ExtraHidePlayingScreenOnEntry] = true
}
