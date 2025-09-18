package remix.myplayer.compose.lyric

data class CurrentNextLyricsLine(
  val currentLine: LyricsLine?,
  val currentLineProgress: Double?,
  val nextLine: LyricsLine?,
) {
  companion object {
    val SEARCHING = CurrentNextLyricsLine(LyricsLine.Companion.LYRICS_LINE_SEARCHING, null, null)
  }
}
