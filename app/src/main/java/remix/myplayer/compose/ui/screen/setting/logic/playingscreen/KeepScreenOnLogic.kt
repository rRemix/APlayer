package remix.myplayer.compose.ui.screen.setting.logic.playingscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.SettingViewModel

@Composable
fun KeepScreenOnLogic() {
  val vm = activityViewModel<SettingViewModel>()
  var screenAlwaysOn by remember { mutableStateOf(vm.settingPrefs.keepScreenOn) }

  SwitchPreference(
    stringResource(R.string.screen_always_on_title),
    stringResource(R.string.screen_always_on_tip),
    screenAlwaysOn
  ) {
    // TODO
    screenAlwaysOn = it
    vm.settingPrefs.keepScreenOn = it
  }
}