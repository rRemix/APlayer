package remix.myplayer.ui.screen.setting.logic.other

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.Preference
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.viewmodel.settingViewModel

/**
 * 播放统计设置：开关采集 + 清除统计数据。
 */
@Composable
fun PlayEventSettingLogic() {
  val settingVM = settingViewModel
  val scope = rememberCoroutineScope()
  val clearState = rememberDialogState()

  SwitchPreference(
    title = stringResource(R.string.play_stat_enabled),
    content = stringResource(R.string.play_stat_enabled_desc),
    checked = settingVM.settingPrefs.playEventEnabled,
    onCheckedChange = {
      settingVM.settingPrefs.playEventEnabled = it
    }
  )

  Preference(
    onClick = { clearState.show() },
    title = stringResource(R.string.clear_play_stats)
  )

  NormalDialog(
    dialogState = clearState,
    titleRes = R.string.confirm_clear_play_stats,
    onPositive = {
      scope.launch {
        settingVM.clearPlayEvents()
        clearState.dismiss()
      }
    }
  )
}
