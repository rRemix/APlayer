package remix.myplayer.compose.ui.screen.setting.logic.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.util.ToastUtil

private val itemRes = listOf(R.string.use_system_color, R.string.use_black_color)

@Composable
fun NotifyBackgroundLogic() {
  val vm = activityViewModel<SettingViewModel>()
  val context = LocalContext.current
  var select by remember {
    mutableIntStateOf(if (vm.settingPrefs.notifyUseSystemBackground) 0 else 1)
  }
  val state = rememberDialogState()
  NormalPreference(
    stringResource(R.string.notify_bg_color),
    stringResource(R.string.notify_bg_color_info)
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.notify_bg_color,
    itemRes = itemRes,
    positiveRes = null,
    negativeRes = null, itemsCallbackSingleChoice = ItemsCallbackSingleChoice(select) {
      if (!vm.settingPrefs.classicNotify) {
        ToastUtil.show(context, R.string.notify_bg_color_warnning)
        return@ItemsCallbackSingleChoice
      }
      select = it
      vm.settingPrefs.notifyUseSystemBackground = select == 0
    }
  )
}