package remix.myplayer.compose.ui.screen.setting.logic.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
fun ClassicNotifyLogic() {
  val vm = settingViewModel

  var classicNotification by remember { mutableStateOf(vm.settingPrefs.classicNotify) }
  SwitchPreference(
    stringResource(R.string.notify_style),
    stringResource(R.string.notify_style_tip),
    classicNotification
  ) {
    classicNotification = it
    vm.settingPrefs.classicNotify = it
  }
}