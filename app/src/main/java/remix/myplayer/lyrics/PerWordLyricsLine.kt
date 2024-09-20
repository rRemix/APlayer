package remix.myplayer.lyrics

import android.text.SpannedString
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import kotlinx.serialization.Serializable
import remix.myplayer.ui.misc.PartialForegroundColorSpan

@Serializable
data class PerWordLyricsLine(
  override val time: Int, val words: List<Word>, override val translation: String? = null
) : LyricsLine() {
  init {
    require(words.isNotEmpty() && time <= words[0].time)
  }

  override val content = words.joinToString("") { it.content }

  override fun withTranslation(newTranslation: String?): PerWordLyricsLine {
    return PerWordLyricsLine(time, words, newTranslation)
  }

  /**
   * @return 0 到 `words.size` 之间的值
   */
  fun getProgress(time: Int, endTime: Int): Float {
    require(time >= this.time && time <= endTime)
    var index = words.binarySearchBy(time) { it.time }
    // TODO: check
    if (index >= 0) {
      return index.toFloat()
    }
    index = -(index + 1)
    check(index >= 0 && index <= words.size)
    return if (index == 0) {
      0f
    } else {
      index - 1 + (time - words[index - 1].time).toFloat() / ((if (index == words.size) endTime else words[index].time) - words[index - 1].time)
    }
  }

  /**
   * @param progress 0 到 `words.size` 之间的值，可通过 `getProgress` 获得
   * @param color 高亮部分的颜色
   */
  fun getSpannedString(progress: Float, @ColorInt color: Int): SpannedString {
    require(progress >= 0 && progress <= words.size)
    return buildSpannedString {
      words.forEachIndexed { index, word ->
        inSpans(PartialForegroundColorSpan((progress - index).coerceIn(0f, 1f), color)) {
          append(word.content)
        }
      }
    }
  }
}
