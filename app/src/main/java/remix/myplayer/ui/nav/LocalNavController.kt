package remix.myplayer.ui.nav

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = compositionLocalOf<NavHostController> {
  error("No LocalNavController provided")
}