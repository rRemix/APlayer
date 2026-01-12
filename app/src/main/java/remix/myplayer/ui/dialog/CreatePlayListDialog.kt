package remix.myplayer.ui.dialog

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteSongChoose
import remix.myplayer.viewmodel.libraryViewModel

@Composable
fun CreatePlayListDialog() {
  val libraryVM = libraryViewModel
  val createPlaylistState by libraryVM.createPlaylistState.collectAsStateWithLifecycle()
  val navController = LocalNavController.current

  InputDialog(
    dialogState = createPlaylistState.dialogState,
    title = stringResource(R.string.new_playlist),
    positive = stringResource(R.string.create),
    text = createPlaylistState.name,
    onDismissRequest = {
      libraryVM.updateNewPlaylistName("")
    },
    onValueChange = {
      libraryVM.updateNewPlaylistName(it)
    }
  ) {
    libraryVM.insertPlayList(it) { id ->
      if (id > 0) {
        navController.navigate("$RouteSongChoose/${id}/${Uri.encode(it)}")
      }
    }
  }
}