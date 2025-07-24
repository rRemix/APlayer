package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.libraryViewModel

@Composable
fun BreakPointLogic() {
  val setting = libraryViewModel.settingPrefs

  var breakPoint by remember { mutableStateOf(setting.playAtBreakPoint) }
  SwitchPreference(
    stringResource(R.string.play_breakpoint),
    stringResource(R.string.play_breakpoint_tip),
    breakPoint
  ) {
    breakPoint = it
    setting.playAtBreakPoint = it
  }
}