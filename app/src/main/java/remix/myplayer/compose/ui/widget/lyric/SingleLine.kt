package remix.myplayer.compose.ui.widget.lyric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.PerWordLyricsLine
import remix.myplayer.compose.prefs.DesktopLyricPrefs.Companion.ELLIPSIS
import kotlin.math.floor

@Composable
fun SingleLine(
  sungColor: Color,
  unSungColor: Color,
  fontSize: TextUnit,
  progress: Double?,
  line: LyricsLine?
) {
  val textMeasurer = rememberTextMeasurer()
  val content = (line?.content ?: "").ifBlank { ELLIPSIS }
  val baseStyle = TextStyle(fontSize = fontSize)

  val isPerWord = line is PerWordLyricsLine

  Layout(
    modifier = Modifier.drawBehind {
      if (content.isEmpty()) return@drawBehind

      // 外层整体裁剪，防止越界到父容器
      clipRect(left = 0f, top = 0f, right = size.width.toFloat(), bottom = size.height.toFloat()) {

        val layoutUnsung = textMeasurer.measure(
          text = content,
          style = baseStyle.copy(color = unSungColor),
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        val layoutSung = textMeasurer.measure(
          text = content,
          style = baseStyle.copy(color = sungColor),
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )

        val textWidth = layoutUnsung.size.width.toFloat()
        val textHeight = layoutUnsung.size.height.toFloat()
        val dy = ((size.height - textHeight) / 2f).coerceAtLeast(0f)

        if (!isPerWord) {
          // 普通歌词：居中或滚动
          val areaW = size.width.toFloat()
          val minDx = areaW - textWidth
          val p = (progress?.toFloat() ?: 0f).coerceIn(0f, 1f)

          val dx = if (areaW >= textWidth) {
            (areaW - textWidth) / 2f
          } else {
            (minDx * p).coerceIn(minDx, 0f)
          }
          drawText(layoutSung, topLeft = Offset(dx, dy))
        } else {
          // 逐词歌词：高亮边界居中跟随 + 左右分区不重叠绘制
          val areaW = size.width.toFloat()

          if (progress != null) {
            val words = line.words

            // 累积每个词的起始字符索引
            var acc = 0
            val starts = IntArray(words.size) { i ->
              val s = acc
              acc += words[i].content.length
              s
            }

            // 进度拆分为整词索引 + 当前词的小数
            val clamped = progress.coerceIn(0.0, words.size.toDouble())
            val index = floor(clamped).toInt().coerceIn(0, words.size)
            val frac = (clamped - index).toFloat().coerceIn(0f, 1f)

            // 已唱边界
            val textLength = content.length
            fun leftXAt(offset: Int): Float = when {
              offset <= 0 -> 0f
              offset >= textLength -> textWidth
              else -> layoutSung.getBoundingBox(offset).left
            }
            fun rightXAt(offset: Int): Float = when {
              offset <= 0 -> 0f
              offset >= textLength -> textWidth
              else -> layoutSung.getBoundingBox(offset - 1).right
            }

            val highlightWidth = if (index >= words.size) {
              textWidth
            } else {
              val start = starts[index]
              val end = start + words[index].content.length
              val xStart = leftXAt(start)
              val xEnd = rightXAt(end)
              val wordWidth = (xEnd - xStart).coerceAtLeast(0f)
              (xStart + wordWidth * frac).coerceIn(0f, textWidth)
            }

            // 文本超宽时，保持 cut 在视图中间；否则居中
            val dx = if (textWidth > areaW) {
              (areaW / 2f - highlightWidth).coerceIn(areaW - textWidth, 0f)
            } else {
              (areaW - textWidth) / 2f
            }

            // 画布坐标下的左右边界与切分位置
            val leftBound = dx
            val rightBound = dx + textWidth
            val cut = dx + highlightWidth
            val pad = 1f

            when {
              highlightWidth <= 0f -> {
                // 全未唱
                clipRect(
                  left = leftBound - 0.5f, top = dy - pad,
                  right = rightBound + 0.5f, bottom = dy + textHeight + pad
                ) { drawText(layoutUnsung, topLeft = Offset(dx, dy)) }
              }
              highlightWidth >= textWidth -> {
                // 全已唱
                clipRect(
                  left = leftBound - 0.5f, top = dy - pad,
                  right = rightBound + 0.5f, bottom = dy + textHeight + pad
                ) { drawText(layoutSung, topLeft = Offset(dx, dy)) }
              }
              else -> {
                // 左半：已唱
                clipRect(
                  left = (leftBound - 0.5f).coerceAtMost(cut),
                  top = dy - pad,
                  right = cut + 0.5f,
                  bottom = dy + textHeight + pad
                ) { drawText(layoutSung, topLeft = Offset(dx, dy)) }
                // 右半：未唱
                clipRect(
                  left = cut - 0.5f,
                  top = dy - pad,
                  right = rightBound + 0.5f,
                  bottom = dy + textHeight + pad
                ) { drawText(layoutUnsung, topLeft = Offset(dx, dy)) }
              }
            }
          } else {
            // 无进度或数据异常，默认居中
            val dx = (areaW - textWidth) / 2f
            drawText(layoutUnsung, topLeft = Offset(dx, dy))
          }
        }
      }
    }
  ) { _, constraints ->
    // 仅测量不绘制
    val layoutResult = textMeasurer.measure(
      text = content,
      style = baseStyle,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )

    val desiredWidth = layoutResult.size.width
    val desiredHeight = layoutResult.size.height

    val width = desiredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

    layout(width, height) {
    }
  }
}