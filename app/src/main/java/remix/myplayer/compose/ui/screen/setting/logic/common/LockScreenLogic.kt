package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.util.Constants

private val itemRes = listOf(
  R.string.aplayer_lockscreen,
  R.string.system_lockscreen,
  R.string.close
)

@Composable
fun LockScreenLogic() {
  val setting = libraryViewModel.settingPrefs

  val lockScreenState = rememberDialogState(false)
  var select by remember {
    mutableIntStateOf(setting.lockScreen)
  }
  NormalPreference(
    stringResource(R.string.lockscreen_show), content = stringResource(
      when (select) {
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
    itemRes = itemRes,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(setting.lockScreen) {
      if (select == it) {
        return@ItemsCallbackSingleChoice
      }
      select = it
      setting.lockScreen = it
    }
  )
}