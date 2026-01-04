package remix.myplayer.lyric

import android.annotation.SuppressLint
import androidx.core.text.HtmlCompat
import remix.myplayer.util.EncodingDetect
import timber.log.Timber
import java.nio.charset.Charset
import kotlin.math.roundToLong

object LrcParser {
  private const val TAG = "LrcParser"

  private val WORD_TIME_TAG_REGEX = """<(\d+:){1,2}\d+(\.\d*)?>""".toRegex()
  private val EMPTY_LINE_WITH_TIME_REGEX = """(\[(\d+:){1,2}\d+(\.\d*)?])*""".toRegex()

  /**
   * 一般格式: `mm:ss.xx`, `mm:ss.xxx`
   *
   * @param offset 正数表示更早，负数表示更晚
   * @return 以毫秒为单位；失败返回 null
   */
  private fun parseTime(timeStr: String, offset: Long): Long? {
    try {
      val parts = timeStr.split(':')
      val minutes = when (parts.size) {
        2 -> parts[0].toLong()
        3 -> parts[0].toLong() * 60 + parts[1].toLong()
        else -> throw Exception("Unknown time format")
      }
      val seconds = minutes * 60 + parts.last().toDouble()
      return (seconds * 1000).roundToLong() - offset
    } catch (t: Throwable) {
      Timber.tag(TAG).w(t, "Failed to parse time: $timeStr")
    }
    return null
  }

  /**
   * 解析精确到字的歌词
   *
   * @param time 整行的开始时间
   */
  private fun parseWords(time: Long, offset: Long, content: String): LyricLine {
    val words = ArrayList<Word>()
    var currentTime = time
    var lastStart = 0
    var match =
      WORD_TIME_TAG_REGEX.find(content) ?: return SimpleLyricLine(time, decodeEntities(content))
    while (true) {
      words.add(
        Word(
          currentTime,
          decodeEntities(content.substring(lastStart, match.range.first))
        )
      )
      parseTime(match.value.substring(1, match.value.lastIndex), offset)?.let {
        // 确保同一 LyricsLine 内 time 单调不减
        if (it > currentTime) {
          currentTime = it
        }
      }
      lastStart = match.range.last + 1
      match = match.next() ?: break
    }
    words.add(Word(currentTime, decodeEntities(content.substring(lastStart))))
    return PerWordLyricLine(time, words)
  }

  fun parse(data: ByteArray): ArrayList<LyricLine> {
    val encoding = runCatching { EncodingDetect.getJavaEncode(data) }.getOrDefault("UTF-8")
    val content = runCatching { String(data, Charset.forName(encoding)) }
      .getOrElse {
        Timber.tag(TAG).w(it, "Failed to decode lyrics with detected encoding: $encoding")
        String(data)
      }
    return parse(content)
  }

  fun parse(data: String): ArrayList<LyricLine> {
    val lines = ArrayList<LyricLine>()
    var offset = 0L

    data.lines().forEach {
      if (it.isBlank()) {
        return@forEach
      }
      if (!it.startsWith('[')) {
        Timber.tag(TAG).w("Ignored unknown line: $it")
        return@forEach
      }

      // [xxx]
      // 特判空行
      if (it.endsWith(']') && !EMPTY_LINE_WITH_TIME_REGEX.matches(it)) {
        val tag = it.substring(1, it.lastIndex)
        // [offset:+/-xxx]
        if (tag.startsWith("offset:")) {
          try {
            offset = tag.substring(7).trim().toLong()
          } catch (t: Throwable) {
            Timber.tag(TAG).w("Failed to parse offset, raw tag: $tag")
          }
          return@forEach
        }
        Timber.tag(TAG).v("Ignored unknown tag: $tag")
        return@forEach
      }

      var index = 0

      // 解析时间戳
      val times = ArrayList<Long>()
      while (it.startsWith("[", index)) {
        val closing = it.indexOf(']', index)
        if (closing == -1) break
        parseTime(it.substring(index + 1, closing), offset)?.let { time ->
          times.add(time)
        }
        index = closing + 1
      }

      val lineContent = decodeEntities(it.substring(index))
      if (times.size == 1) {
        lines.add(parseWords(times[0], offset, it.substring(index)))
      } else {
        times.forEach { time ->
          lines.add(SimpleLyricLine(time, lineContent))
        }
      }
    }

    // 合并翻译
    // 相同时间戳的两行，认为第二行是第一行的翻译
    lines.sortBy { it.time }
    val combinedLines = ArrayList<LyricLine>()
    for (line in lines) {
      val lastLine = combinedLines.lastOrNull()
      combinedLines.add(
        if (lastLine?.time == line.time && lastLine.translation == null) {
          combinedLines.removeAt(combinedLines.lastIndex)
          lastLine.withTranslation(line.content)
        } else {
          line
        }
      )
    }

    return combinedLines
  }

  /**
   * 毫秒转 mm:ss.xx（两位小数）
   */
  @SuppressLint("DefaultLocale")
  fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
  }

  /**
   * 处理歌词中的 HTML 转义字符（如 &apos;），保证逐词歌词显示正常
   */
  private fun decodeEntities(text: String): String {
    if (!text.contains('&')) return text
    return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
  }
}
