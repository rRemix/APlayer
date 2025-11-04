package remix.myplayer.ui.widget.lyric

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText

object PerWordLyricHelper {
  /**
   * endIndex：到当前为止已完成的“整字符数量”
   * fraction：当前字符内的小数进度（0f..1f），用于绘制该字符的部分填充
   */
  data class PerCharProgress(val endIndex: Int, val fraction: Float)

  /**
   * 已完成的整字符数 + 当前字符内的小数进度
   */
  fun computeEndCharProgress(wordTexts: List<String>, progress: Double): PerCharProgress {
    val p = progress.coerceIn(0.0, wordTexts.size.toDouble())

    // 完整部分
    val index = p.toInt()
    // 小数部分
    val inWordFraction = (wordTexts.getOrNull(index)?.length ?: 0) * (p - index).toFloat()

    return PerCharProgress(
      wordTexts.take(index).sumOf(String::length) + inWordFraction.toInt(),
      inWordFraction - inWordFraction.toInt()
    )
  }

  /**
   * 先绘制未唱文本，再按字符边界叠加已唱颜色
   */
  fun DrawScope.drawPerWordOverlay(
    layoutUnsung: TextLayoutResult,
    layoutSung: TextLayoutResult,
    contentLength: Int,
    endIndex: Int,
    endFraction: Float
  ) {
    // 未开始只绘制未唱文本
    if (endIndex <= 0 && endFraction <= 0f) {
      drawText(layoutUnsung)
      return
    }
    // 已经全部完成只绘制已唱文本
    if (endIndex >= contentLength && endFraction <= 0f) {
      drawText(layoutSung)
      return
    }

    // 先绘制未唱，通过裁剪叠加已唱颜色
    drawText(layoutUnsung)

    val whole = endIndex.coerceIn(0, contentLength)

    if (whole > 0) {
      // 先绘制已完成的整字符部分
      val pathWhole = layoutSung.getPathForRange(0, whole)
      clipPath(pathWhole) { drawText(layoutSung) }
    }

    // 处理当前字符的小数部分
    if (endFraction > 0f && whole < contentLength) {
      val rect = layoutSung.getBoundingBox(whole)
      val partialRight = rect.left + rect.width * endFraction

      val lineIdx = layoutSung.getLineForOffset(whole)
      val left = layoutSung.getLineLeft(lineIdx)
      val top = layoutSung.getLineTop(lineIdx)
      val bottom = layoutSung.getLineBottom(lineIdx)

      clipRect(left = left, top = top, right = partialRight, bottom = bottom) {
        drawText(layoutSung)
      }
    }
  }
}