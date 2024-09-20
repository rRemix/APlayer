package remix.myplayer.ui.misc

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * 从左边开始对文字的一部分使用特定颜色的 Span
 *
 * @param proportion 0 到 1 之间的值，表示特定颜色部分的宽度占比
 *
 * TODO: RTL?
 */
class PartialForegroundColorSpan(
  private val proportion: Float,
  @ColorInt private val color: Int
) : ReplacementSpan() {
  init {
    require(proportion in 0f..1f)
  }

  override fun getSize(
    paint: Paint,
    text: CharSequence, start: Int, end: Int,
    fm: Paint.FontMetricsInt?
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

    if (proportion == 0f) {
      canvas.drawText(text, start, end, x, y.toFloat(), paint)
      return
    }
    if (proportion == 1f) {
      paint.color = color
      canvas.drawText(text, start, end, x, y.toFloat(), paint)
      return
    }

    canvas.save()
    canvas.clipRect(x, top.toFloat(), x + width * proportion, bottom.toFloat())
    canvas.drawText(text, start, end, x, y.toFloat(), paint)
    canvas.restore()

    paint.color = color
    canvas.save()
    canvas.clipRect(x + width * proportion, top.toFloat(), x + width, bottom.toFloat())
    canvas.drawText(text, start, end, x, y.toFloat(), paint)
    canvas.restore()
  }
}
