package remix.myplayer.ui.widget.lyric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import remix.myplayer.data.prefs.DesktopLyricPrefs.Companion.ELLIPSIS
import remix.myplayer.lyric.LyricLine
import remix.myplayer.lyric.PerWordLyricLine
import remix.myplayer.ui.widget.lyric.PerWordLyricHelper.drawPerWordOverlay

@Composable
fun LyricSingleLine(
  sungColor: Color,
  unSungColor: Color,
  fontSize: TextUnit,
  useShadow: Boolean = true,
  progress: Double?,
  line: LyricLine?
) {
  val textMeasurer = rememberTextMeasurer()
  val content = (line?.content ?: "").ifBlank { ELLIPSIS }
  val baseStyle = TextStyle(
    fontSize = fontSize,
    fontWeight = FontWeight.Bold,
    shadow = if (useShadow) {
      Shadow(
        color = Color.Black,
        offset = Offset(1f, 1f),
        blurRadius = 2f
      )
    } else {
      null
    }
  )
  val isPerWord = line is PerWordLyricLine

  Layout(
    modifier = Modifier.drawBehind {
      if (content.isEmpty()) return@drawBehind

      // 外层整体裁剪，防止越界到父容器
      clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {

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
        val areaW = size.width.toFloat()

        if (!isPerWord || progress == null) {
          // 普通歌词：居中或滚动
          val minDx = areaW - textWidth
          val p = (progress?.toFloat() ?: 0f).coerceIn(0f, 1f)

          val dx = if (areaW >= textWidth) {
            (areaW - textWidth) / 2f
          } else {
            (minDx * p).coerceIn(minDx, 0f)
          }
          drawText(layoutSung, topLeft = Offset(dx, dy))
        } else {
          val p =
            PerWordLyricHelper.computeEndCharProgress(line.words.map { it.content }, progress)

          val textWidth = layoutUnsung.size.width.toFloat()
          val textLength = content.length
          val whole = p.endIndex.coerceIn(0, textLength)

          val cutX = when {
            p.endIndex <= 0 && p.fraction <= 0f -> 0f
            p.endIndex >= textLength && p.fraction <= 0f -> textWidth
            else -> {
              val rect = layoutSung.getBoundingBox(whole)
              rect.left + rect.width * p.fraction
            }
          }

          // 文本超宽时，保持 cut 在视图中间；否则居中
          val dx = if (textWidth > areaW) {
            (areaW / 2f - cutX).coerceIn(areaW - textWidth, 0f)
          } else {
            (areaW - textWidth) / 2f
          }

          translate(left = dx, top = dy) {
            drawPerWordOverlay(
              layoutUnsung = layoutUnsung,
              layoutSung = layoutSung,
              contentLength = textLength,
              endIndex = p.endIndex,
              endFraction = p.fraction
            )
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

    val width = layoutResult.size.width.coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = layoutResult.size.height.coerceIn(constraints.minHeight, constraints.maxHeight)

    layout(width, height) {
    }
  }
}
