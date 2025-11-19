package remix.myplayer.lyric

data class CurrentNextLyricsLine(
  val currentLine: LyricLine?,
  val currentLineProgress: Double?,
  val nextLine: LyricLine?,
) {
  companion object {
    val SEARCHING = CurrentNextLyricsLine(LyricLine.Companion.LYRICS_LINE_SEARCHING, null, null)
  }
}
