package remix.myplayer.helper

import android.content.Context
import remix.myplayer.R
import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.theme.Theme
import remix.myplayer.ui.widget.desktop.DesktopLyricsView
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

  fun getDesktopLyricsContent(
    lyrics: List<LyricsLine>, offset: Int, progress: Int, duration: Int
  ): DesktopLyricsView.Content {
    if (lyrics.isEmpty()) {
      return DesktopLyricsView.Content(LyricsLine.LYRICS_LINE_NO_LRC, null, 1, 1)
    }
    val progressWithOffset = progress + offset
    val index = lyrics.binarySearchBy(progressWithOffset) { it.time }.let {
      if (it < 0) -(it + 1) - 1 else it
    }
    if (index < 0) {
      check(index == -1)
      return DesktopLyricsView.Content(null, lyrics[0], 1, 1)
    }
    check(index < lyrics.size)
    return DesktopLyricsView.Content(
      lyrics[index],
      lyrics.getOrNull(index + 1),
      progressWithOffset,
      lyrics.getOrNull(index + 1)?.time ?: (duration + offset)
    )
  }
}
