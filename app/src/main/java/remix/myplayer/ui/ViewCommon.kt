package remix.myplayer.ui

import androidx.compose.runtime.Composable
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState

object ViewCommon {

  @Composable
  fun ShowLyricTipDialog(onPositive: () -> Unit) {
//    if (lyricPrefs.tipShown) {
//      onPositive()
//    } else {
//
//    }

    val state = rememberDialogState(true)
    NormalDialog(
      dialogState = state,
      contentRes = R.string.local_lyric_tip,
      onPositive = {
        onPositive()
      }
    )
  }

}