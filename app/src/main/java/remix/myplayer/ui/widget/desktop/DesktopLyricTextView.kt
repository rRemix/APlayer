package remix.myplayer.ui.widget.desktop

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import remix.myplayer.lyric.bean.LrcRow

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/11 14:22
 */
class DesktopLyricTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatTextView(context, attrs, defStyleAttr) {
  /**
   * 当前x坐标
   */
  private var curTextXForHighLightLrc = 0f

  /**
   * 当前的歌词
   */
  private var curLrcRow: LrcRow? = null

  /**
   * 当前歌词的字符串所占的空间
   */
  private val textRect = Rect()

  /**
   * 控制歌词水平滚动的属性动画
   */
  private var animator: ValueAnimator? = null

  /**
   * 垂直偏移
   */
  private var offsetY = 0

  /***
   * 监听属性动画的数值值的改变
   */
  private val updateListener = AnimatorUpdateListener { animation ->
    curTextXForHighLightLrc = animation.animatedValue as Float
    invalidate()
  }

  private fun init() {}

  /**
   * 开始水平滚动歌词
   *
   * @param endX 歌词第一个字的最终的x坐标
   * @param duration 滚动的持续时间
   */
  private fun startScrollLrc(endX: Float, duration: Long) {
    if (animator == null) {
      animator = ValueAnimator.ofFloat(0f, endX)
      animator?.addUpdateListener(updateListener)
    } else {
      curTextXForHighLightLrc = 0f
      animator?.cancel()
      animator?.setFloatValues(0f, endX)
    }
    animator?.duration = duration
    //延迟执行属性动画
    val delay = (duration * 0.1).toLong()
    animator?.startDelay = if (delay > DELAY_MAX) DELAY_MAX.toLong() else delay
    animator?.start()
  }

  override fun setTextColor(colors: ColorStateList) {
    super.setTextColor(colors)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    canvas.drawText(curLrcRow?.content ?: return,
        curTextXForHighLightLrc,
        (height - paint.fontMetrics.top - paint.fontMetrics.bottom) / 2,
        paint)
  }

  fun setLrcRow(lrcRow: LrcRow) {
    if (lrcRow.time != 0 && curLrcRow != null && curLrcRow?.time == lrcRow.time) {
      return
    }
    curLrcRow = lrcRow
    stopAnimation()
    curLrcRow?.let { curLrcRow ->
      //        setText(mCurLrcRow.getContent());
      val paint = paint ?: return
      val text = curLrcRow.content
      paint.getTextBounds(text, 0, text.length, textRect)
      val textWidth = textRect.width().toFloat()
      offsetY = ((textRect.bottom + textRect.top - paint.fontMetrics.bottom - paint
          .fontMetrics.top) / 2).toInt()
      if (textWidth > width) {
        //如果歌词宽度大于view的宽，则需要动态设置歌词的起始x坐标，以实现水平滚动
        startScrollLrc(width - textWidth, (curLrcRow.totalTime * 0.85).toLong())
      } else {
        //如果歌词宽度小于view的宽，则让歌词居中显示
        curTextXForHighLightLrc = (width - textWidth) / 2
        invalidate()
      }
    }

  }

  fun stopAnimation() {
    if (animator != null && animator?.isRunning == true) {
      animator?.cancel()
    }
    invalidate()
  }

  companion object {
    private const val DELAY_MAX = 100
  }

  init {
    init()
  }
}