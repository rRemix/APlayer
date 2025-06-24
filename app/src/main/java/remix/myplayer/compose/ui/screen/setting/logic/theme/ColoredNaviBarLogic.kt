package remix.myplayer.compose.ui.screen.setting.logic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.ui.theme.LocalThemeController

@Composable
fun ColoredNaviBarLogic() {
  val controller = LocalThemeController.current

  var coloredNaviBar by remember { mutableStateOf(controller.appTheme.coloredNaviBar) }
  SwitchPreference(
    stringResource(R.string.navigation_color),
    stringResource(R.string.navigation_is_show),
    coloredNaviBar
  ) {
    coloredNaviBar = it
    controller.setColoredNaviBar(it)
  }
}