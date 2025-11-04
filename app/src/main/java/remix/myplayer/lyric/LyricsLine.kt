package remix.myplayer.lyric

import kotlinx.serialization.Serializable
import remix.myplayer.App
import remix.myplayer.R
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
sealed class LyricsLine {
  /**
   * 这行歌词的开始时间
   */
  abstract val time: Long

  /**
   * 整行歌词的内容，仅文本
   */
  abstract val content: String

  abstract val translation: String?

  abstract fun withTranslation(newTranslation: String?): LyricsLine

  val formattedTime by lazy {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(time) -
        TimeUnit.MINUTES.toSeconds(minutes)
    val millis = time % 1000
    String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, millis)
  }

  companion object {
    val LYRICS_LINE_SEARCHING by lazy {
      SimpleLyricsLine(0, App.context.getString(R.string.searching))
    }
    val LYRICS_LINE_NO_LRC by lazy {
      SimpleLyricsLine(0, App.context.getString(R.string.no_lrc))
    }
  }
}
