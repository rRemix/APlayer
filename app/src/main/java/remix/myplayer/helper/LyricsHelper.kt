package remix.myplayer.helper

import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.ui.widget.desktop.DesktopLyricsView

object LyricsHelper {
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
