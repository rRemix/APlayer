package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.NormalPreference

@Composable
fun ManualScanLogic() {
  NormalPreference(stringResource(R.string.manual_scan), stringResource(R.string.manual_scan_tip)) {

  }
}