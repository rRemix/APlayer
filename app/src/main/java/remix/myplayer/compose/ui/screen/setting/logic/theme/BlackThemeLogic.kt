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
fun BlackThemeLogic() {
  val controller = LocalThemeController.current
  var blackTheme by remember { mutableStateOf(controller.black) }

  SwitchPreference(
    stringResource(R.string.black_theme),
    stringResource(R.string.black_theme_tip),
    blackTheme
  ) {
    blackTheme = it
    controller.black = it
  }
}