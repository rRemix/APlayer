package remix.myplayer.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import remix.myplayer.R
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.theme.ThemeStore.isLightTheme
import remix.myplayer.theme.ThemeStore.textColorPrimary
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil
import java.util.*
import kotlin.math.abs

/**
 * Created by Remix on 2016/3/7.
 */
/**
 * 设置扫描文件大小的seekbar
 */
class FilterSizeSeekBar : View {
  private var mOnSeekBarChangeListener: OnSeekBarChangeListener? = null
  private var mContext: Context

  /**
   * 控件高度与宽度
   */
  private var mViewWidth = 0
  private var mViewHeight = 0

  /**
   * 提示字体
   */
  private val mTextPaint: Paint by lazy {
    Paint()
  }

  /**
   * 轨道垂直中心点
   */
  private var mTrackCenterY = 0

  /**
   * 整个轨道的颜色
   */
  private var mTrackColor = 0

  /**
   * 整个轨道画笔
   */
  private val mTrackPaint: Paint by lazy {
    Paint()
  }

  /**
   * 已完成轨道的颜色
   */
  private var mProgressColor = 0

  /**
   * 已完成轨道的画笔
   */
  private val mProgressPaint: Paint by lazy {
    Paint()
  }

  /**
   * 文字颜色
   */
  private var mTextColor = 0

  /**
   * 小圆点的画笔
   */
  private val mDotPaint: Paint by lazy {
    Paint()
  }

  /**
   * 小圆点颜色
   */
  private var mDotColor = 0

  /**
   * 轨道的高度与长度
   */
  private var mTrackHeigh = 0
  private var mTrackWidth = 0

  /**
   * 总共几个小圆点
   */
  private var mDotNum = 0

  /**
   * 小圆点宽度
   */
  private var mDotWidth = 0

  /**
   * 两个小圆点之间的距离
   */
  private var mDotBetween = 0

  /**
   * 所有小圆点的坐标
   */
  private val mDotPosition = ArrayList<Int>()

  /**
   * Thumb的高度与宽度
   */
  private var mThumbWidth = 0
  private var mThumbHeight = 0

  /**
   * ThumbDrawable 以及两个状态
   */
  lateinit var mThumbDrawable: StateListDrawable
  private val mThumbNormal: IntArray by lazy {
    intArrayOf(-android.R.attr.state_focused, -android.R.attr.state_pressed,
        -android.R.attr.state_selected, -android.R.attr.state_checked)
  }
  private val mThumbPressed: IntArray by lazy {
    intArrayOf(android.R.attr.state_focused, android.R.attr.state_pressed,
        android.R.attr.state_selected, android.R.attr.state_checked)
  }

  /**
   * Thumb所在位置的中心点
   */
  private var mThumbCenterX = 0
  private var mThumbCenterY = 0

  /**
   * 当前索引
   */
  private var mPosition = 0
  //是否初始化完成
  /**
   * 是否初始化完成
   */
  var isInit = false
    private set

  /**
   * 扫描大小设置常量
   */
  private val mTexts = arrayOf("0", "300k", "500K", "800k", "1MB", "2MB")

  constructor(context: Context) : super(context) {
    mContext = context
    init(null)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    mContext = context
    init(attrs)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    mContext = context
    init(attrs)
  }

  override fun onDraw(canvas: Canvas) {
    //整个轨道
    canvas.drawLine(mThumbWidth.toFloat(), mTrackCenterY.toFloat(), (mTrackWidth + mThumbWidth).toFloat(), mTrackCenterY.toFloat(), mTrackPaint)
    //已完成轨道
    canvas.drawLine(mThumbWidth.toFloat(), mTrackCenterY.toFloat(), mThumbCenterX.toFloat(), mTrackCenterY.toFloat(), mProgressPaint)
    //小圆点与底部文字
    for (i in 0 until mDotNum) {
      canvas.drawCircle(mDotPosition[i].toFloat(), mTrackCenterY.toFloat(), mDotWidth.toFloat(), mDotPaint)
      canvas.drawText(mTexts[i], mDotPosition[i].toFloat(), (mThumbHeight * 2).toFloat(), mTextPaint)
    }
    //thumb
    mThumbDrawable.setBounds(mThumbCenterX - mThumbWidth / 2, 0, mThumbCenterX + mThumbWidth / 2,
        mThumbHeight)
    mThumbDrawable.draw(canvas)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val eventX = event.x.toInt()
    val isUp = event.action == MotionEvent.ACTION_UP

    //设置thumb状态
    mThumbDrawable.state = if (isUp) mThumbNormal else mThumbPressed
    if (eventX > mDotPosition[mDotPosition.size - 1] || eventX < mThumbWidth) {
      invalidate()
      return true
    }
    if (isUp) {
      //寻找与当前触摸点最近的值
      var temp = Int.MAX_VALUE
      for (i in mDotPosition.indices) {
        if (abs(mDotPosition[i] - eventX) < temp) {
          mPosition = i
          temp = abs(mDotPosition[i] - eventX)
        }
      }
      mThumbCenterX = mDotPosition[mPosition]
      mOnSeekBarChangeListener?.onProgressChanged(this, mPosition, true)
    } else {
      mThumbCenterX = eventX
    }
    invalidate()
    return true
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (measuredWidth.also { mViewWidth = it } > 0 && measuredHeight.also { mViewHeight = it } > 0) {
      //计算轨道宽度 两个小圆点之间的距离
      mTrackWidth = mViewWidth - mThumbWidth * 2
      mDotBetween = mTrackWidth / (mDotNum - 1)
      mDotPosition.clear()
      //设置所有小圆点的坐标
      for (i in 0 until mDotNum) {
        mDotPosition.add(mThumbWidth + mDotBetween * i)
      }
      mThumbCenterX = mDotPosition[mPosition]
      isInit = true
    }
  }

  private fun init(attrs: AttributeSet?) {
    isInit = false
    val typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.FilterSizeSeekBar)
    //初始化thumbdrawable及其状态
    var thumb: Drawable? = Theme.getTintThumb(mContext)
    var thumbPress: Drawable? = Theme.getTintThumb(mContext)
    if (thumb == null) {
      thumb = resources.getDrawable(R.drawable.bg_circleseekbar_thumb)
    }
    if (thumbPress == null) {
      thumbPress = resources.getDrawable(R.drawable.bg_circleseekbar_thumb)
    }

    mThumbDrawable = StateListDrawable()
    mThumbDrawable.addState(mThumbNormal, thumb)
    mThumbDrawable.addState(mThumbPressed, thumbPress)

    //计算thumb的大小
    mThumbHeight = mThumbDrawable.intrinsicHeight
    mThumbWidth = mThumbDrawable.intrinsicWidth
    mThumbCenterY = mThumbHeight / 2
    mTrackCenterY = mThumbHeight / 2

    //轨道 已完成轨道 文字颜色
    mTrackColor = ColorUtil.getColor(
        if (isLightTheme) R.color.light_scan_track_color else R.color.dark_scan_track_color)
    mProgressColor = accentColor
    mTextColor = textColorPrimary

    //小圆点数量与宽度
    mDotNum = typedArray
        .getInteger(R.styleable.FilterSizeSeekBar_dotnum, DensityUtil.dip2px(mContext, 3f))
    mDotWidth = typedArray
        .getDimension(R.styleable.FilterSizeSeekBar_dotwidth, DensityUtil.dip2px(mContext, 2f).toFloat()).toInt()

    //轨道高度
    mTrackHeigh = typedArray
        .getDimension(R.styleable.FilterSizeSeekBar_trackheight, DensityUtil.dip2px(mContext, 2f).toFloat()).toInt()

    //小圆点画笔
    mDotColor = ColorUtil.shiftColor(accentColor, 0.8f)
    mDotPaint.isAntiAlias = true
    mDotPaint.color = mDotColor
    mDotPaint.style = Paint.Style.FILL

    //提示文字画笔
    mTextPaint.isAntiAlias = true
    mTextPaint.color = mTextColor
    mTextPaint.style = Paint.Style.STROKE
    mTextPaint.textSize = DensityUtil.dip2px(context, 13f).toFloat()
    mTextPaint.textAlign = Paint.Align.CENTER

    //整个轨道的画笔
    mTrackPaint.isAntiAlias = true
    mTrackPaint.color = mTrackColor
    mTrackPaint.style = Paint.Style.STROKE
    mTrackPaint.strokeWidth = mTrackHeigh.toFloat()

    //已完成轨道的画笔
    mProgressPaint.isAntiAlias = true
    mProgressPaint.color = mProgressColor
    mProgressPaint.style = Paint.Style.STROKE
    mProgressPaint.strokeWidth = mTrackHeigh.toFloat()
    typedArray.recycle()
  }

  val position: Long
    get() = mPosition.toLong()

  fun setPosition(position: Int) {
    var position = position
    if (position > mDotPosition.size) {
      position = mDotPosition.size
    }
    if (position < 0) {
      position = 0
    }
    mPosition = position
    mThumbCenterX = mDotPosition[mPosition]
    invalidate()
  }

  interface OnSeekBarChangeListener {
    fun onProgressChanged(seekBar: FilterSizeSeekBar?, position: Int, fromUser: Boolean)
    fun onStartTrackingTouch(seekBar: FilterSizeSeekBar?)
    fun onStopTrackingTouch(seekBar: FilterSizeSeekBar?)
  }

  fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
    mOnSeekBarChangeListener = l
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
  }

  companion object {
    private const val TAG = "CustomSeekBar"
  }
}