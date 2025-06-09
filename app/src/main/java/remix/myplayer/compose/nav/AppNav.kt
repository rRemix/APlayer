package remix.myplayer.compose.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import remix.myplayer.compose.ui.screen.HomeScreen
import remix.myplayer.compose.ui.screen.setting.SettingScreen

const val RouteHome = "Home"
const val RouteSetting = "Setting"

@Composable
fun AppNav() {
  NavHost(LocalNavController.current, startDestination = RouteHome) {
    composable(RouteHome) {
      HomeScreen()
    }
    composable(RouteSetting) {
      SettingScreen()
    }
  }
}