package remix.myplayer.ui.widget.lyric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import remix.myplayer.data.prefs.DesktopLyricPrefs.Companion.ELLIPSIS
import remix.myplayer.lyric.LyricLine
import remix.myplayer.lyric.PerWordLyricLine
import remix.myplayer.ui.widget.lyric.PerWordLyricHelper.drawPerWordOverlay

@Composable
fun LyricMultiLine(
  sungColor: Color,
  unSungColor: Color,
  fontSize: TextUnit,
  progress: Double?,
  line: LyricLine?
) {
  val textMeasurer = rememberTextMeasurer()
  val content = (line?.content ?: "").ifBlank { ELLIPSIS }
  val baseStyle = TextStyle(fontSize = fontSize, textAlign = TextAlign.Center)
  val isPerWord = line is PerWordLyricLine

  Layout(
    modifier = Modifier.drawBehind {
      if (content.isEmpty()) return@drawBehind

      val measureConstraints = Constraints.fixedWidth(size.width.toInt())
      val layoutUnsung = textMeasurer.measure(
        text = content,
        style = baseStyle.copy(color = unSungColor),
        maxLines = Int.MAX_VALUE,
        softWrap = true,
        overflow = TextOverflow.Clip,
        constraints = measureConstraints
      )
      val layoutSung = textMeasurer.measure(
        text = content,
        style = baseStyle.copy(color = sungColor),
        maxLines = Int.MAX_VALUE,
        softWrap = true,
        overflow = TextOverflow.Clip,
        constraints = measureConstraints
      )

      val textHeight = layoutUnsung.size.height.toFloat()
      val dy = ((size.height - textHeight) / 2f).coerceAtLeast(0f)

      if (isPerWord && progress != null) {
        val words = line.words
        val p = PerWordLyricHelper.computeEndCharProgress(words.map { it.content }, progress)

        translate(top = dy) {
          drawPerWordOverlay(
            layoutUnsung = layoutUnsung,
            layoutSung = layoutSung,
            contentLength = content.length,
            endIndex = p.endIndex,
            endFraction = p.fraction
          )
        }
      } else {
        translate(top = dy) { drawText(layoutSung) }
      }
    }
  ) { _, constraints ->
    val layoutResult = textMeasurer.measure(
      text = content,
      style = baseStyle,
      maxLines = Int.MAX_VALUE,
      softWrap = true,
      overflow = TextOverflow.Clip,
      constraints = Constraints.fixedWidth(constraints.maxWidth)
    )
    val width = constraints.maxWidth
    val height = layoutResult.size.height.coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) { }
  }
}