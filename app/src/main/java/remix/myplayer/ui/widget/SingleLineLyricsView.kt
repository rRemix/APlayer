package remix.myplayer.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.lyrics.PerWordLyricsLine
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
  @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
  companion object {
    private const val ELLIPSIS = Typography.ellipsis.toString()
  }

  @ColorInt
  var sungColor: Int = currentTextColor
    set(@ColorInt value) {
      if (value != field) {
        field = value
        invalidate()
      }
    }

  @ColorInt
  var unsungColor: Int = currentTextColor
    set(@ColorInt value) {
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
    set(value) {
      if (value == field) {
        return
      }
      field = value
      content = lyricsLine?.content ?: ""
      contentDescription = content
      progress = null
      invalidate()
      requestLayout()
    }

  /**
   * 当前进度
   *
   * - `lyricsLine` is `PerWordLyricsLine`：[0, lyricsLine.words.size]
   * - 否则：[0, 1]
   */
  private var progress: Float? = null

  fun setProgress(time: Int, endTime: Int) {
    lyricsLine?.let {
      require(time >= it.time && time <= endTime)
      val newProgress = if (it is PerWordLyricsLine) {
        it.getProgress(time, endTime)
      } else {
        (time - it.time).toFloat() / (endTime - it.time)
      }
      if (newProgress != progress) {
        progress = newProgress
        invalidate()
      }
    }
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
          val left = if (width >= textWidth) {
            (width - textWidth) / 2
          } else {
            (width / 2f - highlightWidth).coerceIn(width - textWidth, 0f)
          }
          canvas.save()
          canvas.clipRect(left, 0f, left + highlightWidth, height.toFloat())
          canvas.drawText(content, left, -fm.top, paint)
          canvas.restore()
          paint.color = unsungColor
          canvas.save()
          canvas.clipRect(left + highlightWidth, 0f, left + textWidth, height.toFloat())
          canvas.drawText(content, left, -fm.top, paint)
          canvas.restore()
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
        canvas.drawText(content, left, -fm.top, paint)
      }
    }
  }
}
