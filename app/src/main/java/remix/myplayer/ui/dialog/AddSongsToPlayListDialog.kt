package remix.myplayer.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun AddSongsToPlayListDialog() {
  val scope = rememberCoroutineScope()
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel

  val allPlaylists by libraryVM.playLists.collectAsStateWithLifecycle()
  val state by settingVM.addSongToPlayListState.collectAsStateWithLifecycle()

  NormalDialog(
    dialogState = state.rootDialogState,
    title = stringResource(R.string.add_to_playlist),
    items = allPlaylists.map { it.name },
    positive = null,
    negative = null,
    neutral = stringResource(R.string.create_playlist),
    onNeutral = {
      state.inputDialogState.show()
    },
    itemsCallback = { _, text ->
      scope.runWithLoading {
        libraryVM.addSongsToPlayList(state.songIds, text)
      }
    })

  InputDialog(
    dialogState = state.inputDialogState,
    text = state.inputText,
    title = stringResource(R.string.new_playlist),
    positive = stringResource(R.string.create),
    negative = stringResource(R.string.cancel),
    content = stringResource(R.string.input_playlist_name),
    onDismissRequest = {
      settingVM.updateImportPlayListState("")
    },
    onValueChange = {
      settingVM.updateImportPlayListState(it)
    }
  ) { input ->
    if (input.isNotBlank()) {
      scope.runWithLoading {
        libraryVM.addSongsToPlayList(state.songIds, input, true)
      }
    }
  }
}
