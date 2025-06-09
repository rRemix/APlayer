package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.helper.ShakeDetector

@Composable
fun ShakeLogic() {
  val setting = activityViewModel<LibraryViewModel>().setting

  var shake by remember { mutableStateOf(setting.shake) }
  SwitchPreference(stringResource(R.string.shake), stringResource(R.string.shake_tip), shake) {
    shake = it
    if (it) {
      ShakeDetector.getInstance().beginListen()
    } else {
      ShakeDetector.getInstance().stopListen()
    }
  }
}