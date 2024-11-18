package remix.myplayer.lyrics

import kotlinx.serialization.Serializable
import remix.myplayer.App
import remix.myplayer.R

@Serializable
sealed class LyricsLine {
  /**
   * 这行歌词的开始时间
   */
  abstract val time: Int

  /**
   * 整行歌词的内容，仅文本
   */
  abstract val content: String

  abstract val translation: String?

  abstract fun withTranslation(newTranslation: String?): LyricsLine

  companion object {
    val LYRICS_LINE_SEARCHING by lazy {
      SimpleLyricsLine(0, App.context.getString(R.string.searching))
    }
    val LYRICS_LINE_NO_LRC by lazy {
      SimpleLyricsLine(0, App.context.getString(R.string.no_lrc))
    }
  }
}
