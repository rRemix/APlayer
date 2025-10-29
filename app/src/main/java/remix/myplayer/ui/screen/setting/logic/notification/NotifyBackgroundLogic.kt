package remix.myplayer.ui.screen.setting.logic.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel

private val itemRes = listOf(R.string.use_system_color, R.string.use_black_color)

@Composable
fun NotifyBackgroundLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val select = if (settingState.notification.notifyUseSystemBackground) 0 else 1
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
      if (select == it) {
        return@ItemsCallbackSingleChoice
      }
      if (!settingState.notification.classicNotify) {
        MessageNotifier.show(R.string.notify_bg_color_warnning)
        return@ItemsCallbackSingleChoice
      }

      settingVM.setNotifyUseSystemBackground(it == 0)
    }
  )
}