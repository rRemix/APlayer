package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference

@Composable
fun BlackListLogic() {
  // TODO
  val blackListState = rememberDialogState(false)
  NormalPreference(stringResource(R.string.blacklist), stringResource(R.string.blacklist_tip)) {
    blackListState.show()
  }
  NormalDialog(
    dialogState = blackListState,
    titleRes = R.string.clear_blacklist_title,
    contentRes = R.string.clear_blacklist_content,
    onPositive = {

    },
    onNegative = {

    }
  )
}