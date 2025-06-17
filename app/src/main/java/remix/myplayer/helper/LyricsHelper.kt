package remix.myplayer.helper

import android.content.Context
import remix.myplayer.R
import remix.myplayer.theme.Theme
import remix.myplayer.util.SPUtil

object LyricsHelper {
  fun showLocalLyricsTip(context: Context, action: () -> Unit) {
    if (!SPUtil.getValue(
        context,
        SPUtil.LYRICS_KEY.NAME,
        SPUtil.LYRICS_KEY.LOCAL_LYRICS_TIP_SHOWN,
        false
      )
    ) {
      Theme.getBaseDialog(context)
          .positiveText(R.string.confirm)
          .onPositive { _, _ ->
            SPUtil.putValue(
              context,
              SPUtil.LYRICS_KEY.NAME,
              SPUtil.LYRICS_KEY.LOCAL_LYRICS_TIP_SHOWN,
              true
            )
            action.invoke()
          }
          .content(R.string.local_lyrics_tip)
          .show()
    } else {
      action.invoke()
    }
  }
}
