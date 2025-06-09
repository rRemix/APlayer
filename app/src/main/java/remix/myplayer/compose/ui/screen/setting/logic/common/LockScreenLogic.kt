package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.util.Constants

@Composable
fun LockScreenLogic() {
  val setting = activityViewModel<LibraryViewModel>().setting

  val lockScreenState = rememberDialogState(false)
  var lockScreenTip by rememberSaveable {
    mutableIntStateOf(setting.lockScreen)
  }
  NormalPreference(
    stringResource(R.string.lockscreen_show), content = stringResource(
      when (lockScreenTip) {
        Constants.APLAYER_LOCKSCREEN -> R.string.aplayer_lockscreen_tip
        Constants.SYSTEM_LOCKSCREEN -> R.string.system_lockscreen_tip
        else -> R.string.lockscreen_off_tip
      }
    )
  ) {
    lockScreenState.show()
  }

  NormalDialog(
    dialogState = lockScreenState,
    titleRes = R.string.lockscreen_show,
    positiveRes = null,
    negativeRes = null,
    itemRes = listOf(
      R.string.aplayer_lockscreen,
      R.string.system_lockscreen,
      R.string.close
    ),
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(setting.lockScreen) {
      lockScreenTip = it
      setting.lockScreen = it
    }
  )
}