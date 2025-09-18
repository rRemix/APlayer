package remix.myplayer.compose.lyric

import kotlinx.serialization.Serializable

@Serializable
data class SimpleLyricsLine(
  override val time: Long,
  override val content: String,
  override val translation: String? = null
) : LyricsLine() {
  override fun withTranslation(newTranslation: String?): SimpleLyricsLine {
    return SimpleLyricsLine(time, content, newTranslation)
  }
}