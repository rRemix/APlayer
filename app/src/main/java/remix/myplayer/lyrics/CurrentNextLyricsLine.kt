package remix.myplayer.lyrics

data class CurrentNextLyricsLine(
  val currentLine: LyricsLine?,
  val currentLineProgress: Double?,
  val nextLine: LyricsLine?,
) {
  companion object {
    val SEARCHING = CurrentNextLyricsLine(LyricsLine.LYRICS_LINE_SEARCHING, null, null)
  }
}
