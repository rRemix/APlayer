package remix.myplayer.compose.ui.dialog

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
fun RemoveSongDialog() {
  val settingVM = settingViewModel
  val state by settingVM.deleteSongState.collectAsStateWithLifecycle()
  val activity = LocalActivity.current

  NormalDialog(
    dialogState = state.dialogState,
    titleRes = state.titleRes,
    itemRes = listOf(R.string.delete_source),
    itemsCallbackMultiChoice = ItemsCallbackMultiChoice(if (state.deleteSource) setOf(0) else emptySet()) { index, selected ->
      settingVM.updateDeleteSongState(deleteSource = selected)
    },
    onPositive = {
      settingVM.deleteSongs(activity as? BaseActivity, state.models, state.deleteSource, state.parent)
    }
  )
}