package remix.myplayer.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.AbsSeekBar
import remix.myplayer.R
import remix.myplayer.theme.ThemeStore
import remix.myplayer.util.DensityUtil
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 圆形 seekbar
 *
 * 用于定时关闭界面
 */
class CircleSeekBar(
  context: Context, attrs: AttributeSet?, defStyleAttr: Int
) : AbsSeekBar(
  context, attrs, defStyleAttr
) {
  private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val activeTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  private var trackWidth: Float = 0F
  private var thumbRadius: Float = 0F

  private var centerX = 0
  private var centerY = 0

  /** 半径 */
  private var radius = 0

  /** 滑过的角度（弧度制） */
  private val rad
    get() = progress * PI * 2 / progressMax

  /** 滑过的角度（角度制） */
  private val deg
    get() = progress * 360.0 / progressMax

  /** 整个圆所在的长方形 */
  private var rectF = RectF()

  private var thumbIsPressed: Boolean = false

  private var progressMax = 0
  private var progress = 0

  var onSeekBarChangeListener: OnSeekBarChangeListener? = null

  interface OnSeekBarChangeListener {
    fun onProgressChanged(
      seekBar: CircleSeekBar?, progress: Int, fromUser: Boolean
    )
  }

  constructor(context: Context) : this(context, null, 0)

  constructor(context: Context, attrs: AttributeSet?) : this(
    context, attrs, 0
  )

  init {
    val typedArray =
      context.obtainStyledAttributes(attrs, R.styleable.CircleSeekBar)
    progressMax =
      typedArray.getInteger(R.styleable.CircleSeekBar_progress_max, 600)
    trackWidth = typedArray.getDimension(
      R.styleable.CircleSeekBar_track_width,
      DensityUtil.dip2px(context, 4F).toFloat()
    )
    thumbRadius = typedArray.getDimension(
      R.styleable.CircleSeekBar_thumb_radius,
      DensityUtil.dip2px(context, 10F).toFloat()
    )
    typedArray.recycle()

    trackPaint.style = Paint.Style.STROKE
    trackPaint.strokeWidth = trackWidth
    trackPaint.color = ThemeStore.accentColor
    trackPaint.alpha = ((255 * 0.24).toInt())

    activeTrackPaint.style = Paint.Style.STROKE
    activeTrackPaint.strokeCap = Paint.Cap.ROUND
    activeTrackPaint.strokeWidth = trackWidth
    activeTrackPaint.color = ThemeStore.accentColor

    thumbPaint.style = Paint.Style.FILL
    thumbPaint.color = ThemeStore.accentColor
  }

  override fun setMax(max: Int) {
    progressMax = max
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    centerX = width / 2
    centerY = height / 2
    radius = min(width / 2 - thumbRadius, height / 2 - thumbRadius).toInt()
    rectF = RectF(
      (centerX - radius).toFloat(),
      (centerY - radius).toFloat(),
      (centerX + radius).toFloat(),
      (centerY + radius).toFloat()
    )
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawCircle(
      centerX.toFloat(), centerY.toFloat(), radius.toFloat(), trackPaint
    )
    canvas.drawArc(
      rectF, -90F, deg.toFloat(), false, activeTrackPaint
    )
    canvas.drawCircle(
      (centerX + sin(rad) * radius).toFloat(),
      (centerY + (-cos(rad)) * radius).toFloat(),
      thumbRadius,
      thumbPaint
    )
  }

  /** 判断点击事件是否有效 */
  private fun isTouchValid(eventX: Float, eventY: Float): Boolean {
    val distance = sqrt(
      (eventX - centerX).toDouble().pow(2.0) + (eventY - centerY).toDouble()
        .pow(2.0)
    )
    return distance >= radius - thumbRadius
  }

  /** 根据点击事件坐标计算并更新 Thumb 位置 */
  private fun snapTouchPosition(eventX: Float, eventY: Float) {
    var rad =
      atan2((eventY - centerY).toDouble(), (eventX - centerX).toDouble())
    // 转换角度，以 12 点方向为 0 度
    rad += if (rad >= -0.5 * PI) {
      0.5 * PI
    } else {
      2.5 * PI
    }
    // 设置当前进度
    progress = (rad / (2.0 * PI) * progressMax).toInt()
    onSeekBarChangeListener?.onProgressChanged(
      this, progress, true
    )
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!isEnabled || !isClickable) return false
    when (event.actionMasked) {
      MotionEvent.ACTION_UP -> thumbIsPressed = false
      MotionEvent.ACTION_DOWN -> {
        if (isTouchValid(event.x, event.y)) {
          thumbIsPressed = true
          snapTouchPosition(event.x, event.y)
          invalidate()
        }
      }
      MotionEvent.ACTION_MOVE -> {
        if (thumbIsPressed && isTouchValid(event.x, event.y)) {
          snapTouchPosition(event.x, event.y)
          invalidate()
        }
      }
    }
    return true
  }

  override fun setProgress(progress: Int) {
    if (progress == this.progress) return
    this.progress = progress
    if (this.progress > progressMax) {
      this.progress = progressMax
    } else if (this.progress < 0) {
      this.progress = 0
    }
    invalidate()
  }

  override fun getProgress(): Int {
    return progress
  }
}