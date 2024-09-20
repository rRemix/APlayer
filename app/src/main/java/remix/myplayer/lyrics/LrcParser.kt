package remix.myplayer.lyrics

import timber.log.Timber
import kotlin.math.roundToInt

object LrcParser {
  private const val TAG = "LrcParser"

  private val WORD_TIME_TAG_REGEX = """<(\d+:){1,2}\d+(\.\d*)?>""".toRegex()

  /**
   * 一般格式: `mm:ss.xx`, `mm:ss.xxx`
   *
   * @param offset 正数表示更早，负数表示更晚
   * @return 以毫秒为单位；失败返回 null
   */
  private fun parseTime(timeStr: String, offset: Int): Int? {
    try {
      val parts = timeStr.split(':')
      val minutes = when (parts.size) {
        2 -> parts[0].toInt()
        3 -> parts[0].toInt() * 60 + parts[1].toInt()
        else -> throw Exception("Unknown time format")
      }
      val seconds = minutes * 60 + parts.last().toDouble()
      return (seconds * 1000).roundToInt() - offset
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
  private fun parseWords(time: Int, offset: Int, content: String): LyricsLine {
    val words = ArrayList<Word>()
    var currentTime = time
    var lastStart = 0
    var match = WORD_TIME_TAG_REGEX.find(content) ?: return SimpleLyricsLine(time, content)
    while (lastStart < content.length) {
      words.add(Word(currentTime, content.substring(lastStart, match.range.first)))
      parseTime(match.value.substring(1, match.value.lastIndex), offset)?.let {
        // 确保同一 LyricsLine 内 time 单调不减
        if (it > currentTime) {
          currentTime = it
        }
      }
      lastStart = match.range.last + 1
      match = match.next() ?: break
    }
    if (lastStart < content.length) {
      words.add(Word(currentTime, content.substring(lastStart)))
    }
    return PerWordLyricsLine(time, words)
  }

  fun parse(data: String): ArrayList<LyricsLine> {
    val lines = ArrayList<LyricsLine>()
    var offset = 0

    data.lines().forEach {
      if (it.isBlank()) {
        return@forEach
      }
      if (!it.startsWith('[')) {
        Timber.tag(TAG).w("Ignored unknown line: $it")
        return@forEach
      }

      // [xxx]
      if (it.endsWith(']')) {
        val tag = it.substring(1, it.lastIndex)
        println(tag)
        // [offset:+/-xxx]
        if (tag.startsWith("offset:")) {
          try {
            offset = Integer.parseInt(tag.substring(7).trim())
            return@forEach
          } catch (_: Throwable) {
          }
        }
        Timber.tag(TAG).v("Ignored unknown tag: $tag")
        return@forEach
      }

      var index = 0

      // 解析时间戳
      val times = ArrayList<Int>()
      while (it.startsWith("[", index)) {
        val closing = it.indexOf(']', index)
        if (closing == -1) break
        parseTime(it.substring(index + 1, closing - 1), offset)?.let { time ->
          times.add(time)
        }
        index = closing + 1
      }

      if (times.size == 1) {
        lines.add(parseWords(times[0], offset, it.substring(index)))
      } else {
        times.forEach { time ->
          lines.add(SimpleLyricsLine(time, it.substring(index)))
        }
      }
    }

    // 合并翻译
    // 相同时间戳的两行，认为第二行是第一行的翻译
    lines.sortBy { it.time }
    val combinedLines = ArrayList<LyricsLine>()
    for (line in lines) {
      val lastLine = combinedLines.lastOrNull()
      combinedLines.add(
        if (lastLine?.time == line.time && lastLine.translation == null) {
          combinedLines.removeLast()
          lastLine.withTranslation(line.content)
        } else {
          line
        }
      )
    }

    return combinedLines
  }
}