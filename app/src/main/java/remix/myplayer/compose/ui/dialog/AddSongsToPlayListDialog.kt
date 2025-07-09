package remix.myplayer.compose.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.util.ToastUtil

@Composable
fun AddSongsToPlayListDialog() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val libraryVM = activityViewModel<LibraryViewModel>()
  val settingVM = activityViewModel<SettingViewModel>()

  val allPlaylists by libraryVM.playLists.collectAsStateWithLifecycle()
  val state by settingVM.importPlayListState.collectAsStateWithLifecycle()

  NormalDialog(
    dialogState = state.baseDialogState,
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
    if (allPlaylists.any { it.name == input }) {
      ToastUtil.show(context, R.string.playlist_already_exist)
    } else if (input.isNotBlank()) {
      scope.runWithLoading {
        libraryVM.addSongsToPlayList(state.songIds, input, true)
      }
    }
  }
}
