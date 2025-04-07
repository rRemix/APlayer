package remix.myplayer.ui.misc

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.core.graphics.withClip
import kotlin.math.roundToInt

/**
 * 从左边开始对文字的一部分使用特定颜色的 Span
 *
 * @param proportion 0 到 1 之间的值，表示特定颜色部分的宽度占比
 *
 * TODO: RTL?
 */
class PartialForegroundColorSpan(
  private val proportion: Double, @ColorInt private val color: Int
) : ReplacementSpan() {
  init {
    require(proportion in 0.0..1.0)
  }

  override fun getSize(
    paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
  ): Int {
    return paint.measureText(text, start, end).roundToInt()
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence, start: Int, end: Int,
    x: Float, top: Int, y: Int, bottom: Int,
    paint: Paint
  ) {
    val width = paint.measureText(text, start, end)

    if (proportion == 0.0) {
      canvas.drawText(text, start, end, x, y.toFloat(), paint)
      return
    }
    if (proportion == 1.0) {
      paint.color = color
      canvas.drawText(text, start, end, x, y.toFloat(), paint)
      return
    }

    val mid = (x + width * proportion).toFloat()
    canvas.withClip(x, top.toFloat(), mid, bottom.toFloat()) {
      drawText(text, start, end, x, y.toFloat(), paint)
    }
    paint.color = color
    canvas.withClip(mid, top.toFloat(), x + width, bottom.toFloat()) {
      drawText(text, start, end, x, y.toFloat(), paint)
    }
  }
}
