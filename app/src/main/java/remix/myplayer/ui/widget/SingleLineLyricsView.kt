package remix.myplayer.ui.widget

import android.R
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.withClip
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.PerWordLyricsLine
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 显示单行歌词（不换行）的 View
 *
 *
 * TODO: RTL支持?
 */
class SingleLineLyricsView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  @AttrRes defStyleAttr: Int = R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
  companion object {
    private const val ELLIPSIS = Typography.ellipsis.toString()
  }

  @ColorInt var sungColor: Int = currentTextColor
    @UiThread set(@ColorInt value) {
      if (value != field) {
        field = value
        invalidate()
      }
    }

  @ColorInt var unsungColor: Int = currentTextColor
    @UiThread set(@ColorInt value) {
      if (value != field) {
        field = value
        invalidate()
      }
    }

  /**
   * 整行的全部文字
   */
  private var content = ""
  var lyricsLine: LyricsLine? = null
    @UiThread set(value) {
      if (value == field) {
        return
      }
      field = value
      // 不能在这里就把 content 换成省略号，不然后面判断会出问题
      content = lyricsLine?.content ?: ""
      contentDescription = content
      if (progress != null) {
        progress = null
        // 已间接调用 invalidate()
      } else {
        invalidate()
      }
      requestLayout()
    }

  /**
   * 当前进度
   *
   * 取值范围：
   * - `lyricsLine` is `PerWordLyricsLine`：[0, lyricsLine.words.size]
   * - 否则：[0, 1]
   */
  var progress: Double? = null
    @UiThread set(value) {
      if (value == field) {
        return
      }
      if (value != null) {
        lyricsLine.let {
          check(it != null)
          require(value in 0.0..(if (it is PerWordLyricsLine) it.words.size.toDouble() else 1.0))
        }
      }
      field = value
      invalidate()
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)

    val width = if (widthMode == MeasureSpec.EXACTLY) {
      widthSize
    } else {
      val desired = paint.measureText(content.ifBlank { ELLIPSIS }).roundToInt()
      if (widthMode == MeasureSpec.AT_MOST) min(desired, widthSize) else desired
    }
    val height = if (heightMode == MeasureSpec.EXACTLY) {
      heightSize
    } else {
      val fm = paint.fontMetrics
      val desired = (fm.bottom - fm.top).roundToInt()
      if (heightMode == MeasureSpec.AT_MOST) min(desired, heightSize) else desired
    }
    setMeasuredDimension(width, height)
  }

  override fun onDraw(canvas: Canvas) {
    // Make compiler happy
    val content = content
    val lyricsLine = lyricsLine
    val offset = progress

    val textWidth = paint.measureText(content.ifBlank { ELLIPSIS })
    val fm = paint.fontMetrics
    paint.color = sungColor
    if (content.isBlank() || lyricsLine == null || offset == null) {
      // 进度未设置时居中显示
      // 无可用内容时显示省略号
      canvas.drawText(content.ifBlank { ELLIPSIS }, (width - textWidth) / 2, -fm.top, paint)
    } else {
      if (lyricsLine is PerWordLyricsLine) {
        val index = offset.toInt()
        if (index < lyricsLine.words.size) {
          val l =
            paint.measureText(lyricsLine.words.subList(0, index).joinToString("") { it.content })
          val r = paint.measureText(
            lyricsLine.words.subList(0, index + 1).joinToString("") { it.content })
          val highlightWidth = l + (r - l) * (offset - index)
          val left = (if (width >= textWidth) {
            (width - textWidth) / 2
          } else {
            (width / 2.0 - highlightWidth).coerceIn((width - textWidth).toDouble(), 0.0)
          }).toFloat()
          val mid = (left + highlightWidth).toFloat()
          canvas.withClip(left, 0f, mid, height.toFloat()) {
            drawText(content, left, -fm.top, paint)
          }
          paint.color = unsungColor
          canvas.withClip(mid, 0f, left + textWidth, height.toFloat()) {
            drawText(content, left, -fm.top, paint)
          }
        } else {
          check(index == lyricsLine.words.size)
          val left = if (width >= textWidth) {
            (width - textWidth) / 2
          } else {
            width - textWidth
          }
          canvas.drawText(content, left, -fm.top, paint)
        }
      } else {
        val left = if (width >= textWidth) {
          (width - textWidth) / 2
        } else {
          (width - textWidth) * offset
        }
        canvas.drawText(content, left.toFloat(), -fm.top, paint)
      }
    }
  }
}
