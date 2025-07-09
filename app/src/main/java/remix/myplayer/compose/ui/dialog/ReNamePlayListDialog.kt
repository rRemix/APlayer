package remix.myplayer.compose.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel

@Composable
fun ReNamePlayListDialog() {
  val settingVM = activityViewModel<SettingViewModel>()
  val libraryVM = activityViewModel<LibraryViewModel>()
  val state by settingVM.reNamePlayListState.collectAsStateWithLifecycle()

  var text by rememberSaveable {
    mutableStateOf("")
  }

  InputDialog(
    dialogState = state.dialogState,
    title = stringResource(R.string.rename),
    text = text,
    onDismissRequest = {
      text = ""
    },
    onValueChange = {
      text = it
    },
    onInput = {
      libraryVM.renamePlayList(state.playListId, text)
      text = ""
    }
  )
}
