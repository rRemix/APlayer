package remix.myplayer.lyric

import kotlinx.serialization.Serializable

@Serializable
data class SimpleLyricLine(
  override val time: Long,
  override val content: String,
  override val translation: String? = null
) : LyricLine() {
  override fun withTranslation(newTranslation: String?): SimpleLyricLine {
    return SimpleLyricLine(time, content, newTranslation)
  }
}