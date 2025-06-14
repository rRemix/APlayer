package remix.myplayer.compose.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import remix.myplayer.compose.ui.screen.HomeScreen
import remix.myplayer.compose.ui.screen.SongChooseScreen
import remix.myplayer.compose.ui.screen.setting.SettingScreen

const val RouteHome = "home"
const val RouteSetting = "setting"
const val RouteSongChoose = "song_choose"

@Composable
fun AppNav() {
  NavHost(LocalNavController.current, startDestination = RouteHome) {
    composable(RouteHome) {
      HomeScreen()
    }
    composable(RouteSetting) {
      SettingScreen()
    }
    composable("${RouteSongChoose}/{id}/{name}", arguments = listOf(navArgument("id") {
      type = NavType.LongType
    })) {
      val id = it.arguments?.getLong("id") ?: return@composable
      val name = it.arguments?.getString("name") ?: return@composable
      SongChooseScreen(id, name)
    }
  }
}