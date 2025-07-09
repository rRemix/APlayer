package remix.myplayer.compose.ui.dialog

import androidx.compose.runtime.Composable

/**
 * some common and reusable dialog
 */
@Composable
fun DialogContainer() {
  AddSongsToPlayListDialog()

  LoadingDialog()

  TimerDialog()

  RemoveSongDialog()

  ReNamePlayListDialog()

  InAppUpdateDialog()
}