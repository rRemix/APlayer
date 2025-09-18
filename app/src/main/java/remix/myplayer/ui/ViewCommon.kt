package remix.myplayer.ui

import android.content.Context
import androidx.compose.runtime.Composable
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.theme.Theme
import remix.myplayer.util.SPUtil

object ViewCommon {
  @Composable
  fun showLyricTipDialog(onPositive: () -> Unit) {
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

  fun showLocalLyricTip(context: Context, action: () -> Unit) {
    if (!SPUtil.getValue(
        context,
        SPUtil.LYRIC_KEY.NAME,
        SPUtil.LYRIC_KEY.LYRIC_LOCAL_TIP_SHOWN,
        false
      )
    ) {
      SPUtil.putValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.LYRIC_LOCAL_TIP_SHOWN, true)
      Theme.getBaseDialog(context)
        .negativeText(R.string.cancel)
        .positiveText(R.string.confirm)
        .onPositive { dialog, which ->
          action.invoke()
        }
        .content(R.string.local_lyric_tip)
        .show()
    } else {
      action.invoke()
    }
  }
  
}