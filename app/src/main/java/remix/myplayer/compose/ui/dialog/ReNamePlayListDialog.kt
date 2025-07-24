package remix.myplayer.compose.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
fun ReNamePlayListDialog() {
  val settingVM = settingViewModel
  val libraryVM = libraryViewModel
  val state by settingVM.reNamePlayListState.collectAsStateWithLifecycle()

  var text by rememberSaveable {
    mutableStateOf("")
  }

  InputDialog(
    dialogState = state.dialogState,
    title = stringResource(R.string.rename),
    text = text,
    onValueChange = {
      text = it
    },
    onInput = {
      val pl = state.playList ?: return@InputDialog
      libraryVM.updatePlayList(pl.copy(name = text))
      text = ""
    }
  )

  LaunchedEffect(state.dialogState.isOpen, state.playList) {
    if (state.dialogState.isOpen && state.playList != null) {
      text = state.playList!!.name
    }
  }
}
