package remix.myplayer.ui

import android.content.Context
import remix.myplayer.R
import remix.myplayer.theme.Theme
import remix.myplayer.util.SPUtil

object ViewCommon {
  fun showLocalLyricTip(context: Context, action: () -> Unit) {
//    if (!SPUtil.getValue(
//        context,
//        SPUtil.LYRIC_KEY.NAME,
//        SPUtil.LYRIC_KEY.LYRIC_LOCAL_TIP_SHOWN,
//        false
//      )
//    ) {
//      SPUtil.putValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.LYRIC_LOCAL_TIP_SHOWN, true)
//      Theme.getBaseDialog(context)
//        .negativeText(R.string.cancel)
//        .positiveText(R.string.confirm)
//        .onPositive { dialog, which ->
//          action.invoke()
//        }
//        .content(R.string.local_lyric_tip)
//        .show()
//    } else {
//      action.invoke()
//    }
    TODO()
  }
}