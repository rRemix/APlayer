package remix.myplayer.ui.dialog

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.R
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun RemoveSongDialog() {
  val settingVM = settingViewModel
  val state by settingVM.deleteSongState.collectAsStateWithLifecycle()
  val activity = LocalActivity.current

  val canDeleteSource = state.models.all { model ->
    when (model) {
      is Song -> model.isLocal()
      is PlayList -> model.songIdentities.all { !it.sourceId.isRemote }
      else -> true
    }
  }
  NormalDialog(
    dialogState = state.dialogState,
    titleRes = state.titleRes,
    itemRes = if (canDeleteSource) listOf(R.string.delete_source) else emptyList(),
    itemsCallbackMultiChoice = ItemsCallbackMultiChoice(if (state.deleteSource) setOf(0) else emptySet()) { index, selected ->
      settingVM.updateDeleteSongState(deleteSource = selected)
    },
    onPositive = {
      settingVM.deleteSongs(activity as? BaseActivity, state.models, state.deleteSource && canDeleteSource, state.parent)
    }
  )
}