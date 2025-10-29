package remix.myplayer.ui.screen.setting.logic.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun ClassicNotifyLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  SwitchPreference(
    stringResource(R.string.notify_style),
    stringResource(R.string.notify_style_tip),
    settingState.notification.classicNotify
  ) {
    settingVM.setClassicNotify(it)
  }
}