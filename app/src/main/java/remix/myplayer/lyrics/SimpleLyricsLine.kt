package remix.myplayer.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class SimpleLyricsLine(
  override val time: Int,
  override val content: String,
  override val translation: String? = null
) : LyricsLine() {
  override fun withTranslation(newTranslation: String?): SimpleLyricsLine {
    return SimpleLyricsLine(time, content, newTranslation)
  }
}