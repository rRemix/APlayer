package remix.myplayer.lyric

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.theme.Theme
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.SPUtil
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Remix on 2018/1/3.
 */
class LrcView : View, ILrcView {
  /**
   * 所有的歌词
   */
  private var lrcRows: List<LrcRow>? = null

  /**
   * 所有歌词总计高度
   */
  private var totalHeight = 0

  /**
   * 画高亮歌词的画笔
   */
  private val highLightPaint by lazy {
    TextPaint()
  }

  /**
   * 高亮歌词当前的字体颜色
   */
  private var highLightTextColor = DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC

  /**
   * 画其他歌词的画笔
   */
  private val normalPaint by lazy {
    TextPaint()
  }
  /**
   * 高亮歌词当前的字体颜色
   */
  private var normalTextColor = DEFAULT_COLOR_FOR_OTHER_LRC

  /**
   * 画时间线的画笔
   */
  private val timeLinePaint: TextPaint by lazy {
    TextPaint()
  }

  /***时间线的颜色 */
  private var timeLineTextColor = Color.GRAY

  /**
   * 时间文字大小
   */
  private var timeLineTextSize = 0f

  /**
   * 是否画时间线
   */
  private var isDrawTimeLine = false

  /**
   * 每一句歌词之间的行距
   */
  private var linePadding = DEFAULT_PADDING
  /**
   * 返回当前的歌词缩放比例
   */
  /**
   * 歌词的当前缩放比例
   */
  var scalingFactor =
    SPUtil.getValue(App.context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.LYRIC_FONT_SIZE, "1f")
      .toFloat()
    private set

  /**
   * 实现歌词竖直方向平滑滚动的辅助对象
   */
  private val scroller: Scroller by lazy {
    Scroller(context, DEFAULT_INTERPOLATOR)
  }

  /**
   * 插值器
   */
  private val DEFAULT_INTERPOLATOR: Interpolator = DecelerateInterpolator()

  /**
   * 控制文字缩放的因子
   */
  private var curFraction = 0f
  private var touchSlop = 0

  /**
   * 错误提示文字
   */
  private var placeholder = App.context.getString(R.string.no_lrc)

  /**
   * 当前纵坐标
   */
  private var rowY = 0f

  /**
   * 时间线的图标
   */
  private val timelineDrawable = Theme
    .getDrawable(App.context, R.drawable.icon_lyric_timeline)

  /**
   * 初始状态时间线图标所在的位置
   */
  private val timelineRect by lazy {
    Rect(
      -timelineDrawable.intrinsicWidth / 2,
      height / 2 - timelineDrawable.intrinsicHeight * 2,
      timelineDrawable.intrinsicWidth * 2,
      height / 2 + timelineDrawable.intrinsicHeight * 2
    )
  }

  constructor(context: Context?) : super(context) {
    init()
  }

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    init()
  }

  /**
   * 初始化画笔等
   */
  override fun init() {
    highLightPaint.isAntiAlias = true
    highLightPaint.color = highLightTextColor

    highLightPaint.textSize = DEFAULT_TEXT_SIZE * scalingFactor
    highLightPaint.isFakeBoldText = true

    normalPaint.isAntiAlias = true
    normalPaint.color = normalTextColor
    normalPaint.textSize = DEFAULT_TEXT_SIZE * scalingFactor

    timeLinePaint.isAntiAlias = true
    timeLineTextSize = TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_SP, 11f,
      context.resources.displayMetrics
    )
    timeLinePaint.textSize = timeLineTextSize
    timeLinePaint.color = timeLineTextColor
    touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    linePadding = DEFAULT_PADDING * scalingFactor
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
//    if (timelineRect == null) { //扩大点击区域
//      TIMELINE_DRAWABLE_RECT = Rect(-TIMELINE_DRAWABLE.intrinsicWidth / 2,
//          height / 2 - TIMELINE_DRAWABLE.intrinsicHeight * 2,
//          TIMELINE_DRAWABLE.intrinsicWidth * 2,
//          height / 2 + TIMELINE_DRAWABLE.intrinsicHeight * 2)
//    }
  }

  @SuppressLint("DrawAllocation")
  override fun onDraw(canvas: Canvas) {
    if (lrcRows == null || lrcRows?.isEmpty() == true) {
      //画默认的显示文字
      val textWidth = normalPaint.measureText(placeholder)
      val textX = (width - textWidth) / 2
      normalPaint.alpha = 0xff
      canvas.drawText(placeholder, textX, (height / 2).toFloat(), normalPaint)
      return
    }
    val availableWidth = width - (paddingLeft + paddingRight)
    rowY = (height / 2).toFloat()
    lrcRows?.let {
      for (i in it.indices) {
        if (i == curRow) {   //画高亮歌词
          drawLrcRow(canvas, highLightPaint, availableWidth, it[i])
        } else {  //普通歌词
          drawLrcRow(canvas, normalPaint, availableWidth, it[i])
        }
      }
    }

    //画时间线和时间
    if (isDrawTimeLine) {
//            final int timeLineOffsetY =
//                    mCurRow >= 0 && mLrcRows != null && mCurRow <= mLrcRows.size() - 1 ?
//                    mLrcRows.get(mCurRow).getTotalHeight() / 2 :
//                    0;
//            float y = getHeight() / 2 + getScrollY() + timeLineOffsetY ;
      val y = height / 2 + scrollY + DEFAULT_SPACING_PADDING
      val lrcRow = lrcRows?.get(curRow)
      if (lrcRow != null) {
        canvas.drawText(
          lrcRow.timeStr,
          width - timeLinePaint.measureText(lrcRow.timeStr) - 5,
          y - 10, timeLinePaint
        )
      }

      canvas.drawLine(
        (timelineDrawable.intrinsicWidth + 10).toFloat(),
        y,
        width.toFloat(),
        y,
        timeLinePaint
      )
      timelineDrawable.setBounds(
        0,
        y.toInt() - timelineDrawable.intrinsicHeight / 2,
        timelineDrawable.intrinsicWidth,
        y.toInt() + timelineDrawable.intrinsicHeight / 2
      )
      timelineDrawable.draw(canvas)
    }
  }

  /**
   * 分割绘制歌词
   */
  private fun drawLrcRow(
    canvas: Canvas,
    textPaint: TextPaint?,
    availableWidth: Int,
    lrcRow: LrcRow
  ) {
    drawText(canvas, textPaint, availableWidth, lrcRow.content)
    if (lrcRow.hasTranslate()) {
//            mRowY += DEFAULT_SPACING_PADDING;
      drawText(canvas, textPaint, availableWidth, lrcRow.translate)
    }
    rowY += linePadding
  }

  /**
   * 分割绘制歌词
   */
  private fun drawText(canvas: Canvas, textPaint: TextPaint?, availableWidth: Int, text: String) {
    val staticLayout = StaticLayout(
      text, textPaint, availableWidth,
      Layout.Alignment.ALIGN_CENTER,
      DEFAULT_SPACING_MULTI, 0f, true
    )
    val extra = if (staticLayout.lineCount > 1) DensityUtil.dip2px(context, 10f) else 0
    canvas.save()
    canvas.translate(paddingLeft.toFloat(), rowY - staticLayout.height / 2 + extra)
    staticLayout.draw(canvas)
    canvas.restore()
    rowY += staticLayout.height.toFloat()
  }

  /**
   * 是否可拖动歌词
   */
  private var canDrag = false

  /**
   * 事件的第一次的y坐标
   */
  private var firstY = 0f

  /**
   * 事件的上一次的y坐标
   */
  private var lastY = 0f
  private var lastX = 0f

  /**
   * 等待TimeLine
   */
  private var timeLineWaiting = false

  /**
   * 长按runnable
   */
  private var longPressRunnable: Runnable? = LongPressRunnable()
  private val timeLineDisableRunnable by lazy {
    TimeLineRunnable()
  }

  private val mHandler by lazy {
    Handler(Looper.getMainLooper())
  }

  private inner class LongPressRunnable : Runnable {
    override fun run() {
      if (onLrcClickListener != null) {
        onLrcClickListener?.onLongClick()
      }
    }
  }

  private inner class TimeLineRunnable : Runnable {
    override fun run() {
      timeLineWaiting = false
      isDrawTimeLine = false
      invalidate()
    }
  }

  fun getLrcRows(): List<LrcRow>? {
    return lrcRows
  }

  private fun hasLrc(): Boolean {
    return lrcRows != null && lrcRows?.isNotEmpty() == true
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        //没有歌词
        if (hasLrc()) {
          firstY = event.rawY
          lastX = event.rawX
          if (timeLineWaiting) {
            //点击定位图标
            if (timelineRect.contains(event.x.toInt(), event.y.toInt())
              && onSeekToListener != null && curRow != -1
            ) {
              mHandler.removeCallbacks(timeLineDisableRunnable)
              mHandler.post(timeLineDisableRunnable)
              onSeekToListener?.onSeekTo(lrcRows?.get(curRow)?.time ?: 0)
              return false
            }
          }
        }
        longPressRunnable = LongPressRunnable()
        mHandler.postDelayed(longPressRunnable!!, ViewConfiguration.getLongPressTimeout().toLong())
      }
      MotionEvent.ACTION_MOVE -> if (hasLrc()) {
        if (!canDrag) {
          if (abs(x = event.rawY - firstY) > touchSlop
            && abs(x = event.rawY - firstY) > abs(x = event.rawX - lastX)
          ) {
            canDrag = true
            isDrawTimeLine = true
            scroller.forceFinished(true)
            curFraction = 1f
          }
          lastY = event.rawY
        }
        if (canDrag) {
          timeLineWaiting = false
          longPressRunnable?.let { mHandler.removeCallbacks(it) }
          val offset = event.rawY - lastY //偏移量
          if (scrollY - offset < 0) {
            if (offset > 0) {
//                                offset = offset / 3;
            }
          } else if (scrollY - offset > totalHeight) {
            if (offset < 0) {
//                                offset = offset / 3;
            }
          }
          scrollBy(scrollX, (-offset).toInt())
          lastY = event.rawY
          //根据滚动后的距离 查找歌词
//                        int currentRow = (int) (getScrollY() / (mSizeForOtherLrc + mLinePadding));
          var currentRow = rowByScrollY
          lrcRows?.let { lrcRows ->
            currentRow = min(currentRow, lrcRows.size - 1)
            currentRow = max(currentRow, 0)
            seekTo(lrcRows[currentRow].time, false, false)
          }
          return true
        }
        lastY = event.rawY
      } else {
        longPressRunnable?.let { mHandler.removeCallbacks(it) }
      }
      MotionEvent.ACTION_UP -> if (!canDrag) {
        if (longPressRunnable == null && onLrcClickListener != null) {
          onLrcClickListener?.onClick()
        }
        longPressRunnable?.let { mHandler.removeCallbacks(it) }
        longPressRunnable = null
      } else {
        //显示三秒TimeLine
        mHandler.removeCallbacks(timeLineDisableRunnable)
        mHandler.postDelayed(timeLineDisableRunnable, DURATION_TIME_LINE.toLong())
        timeLineWaiting = true
        if (scrollY < 0) {
          smoothScrollTo(0, DURATION_FOR_ACTION_UP)
        } else if (scrollY > getScrollYByRow(curRow)) {
          smoothScrollTo(getScrollYByRow(curRow), DURATION_FOR_ACTION_UP)
        }
        canDrag = false
        //                    mIsDrawTimeLine = false;
        invalidate()
      }
      MotionEvent.ACTION_CANCEL -> {
        longPressRunnable?.let { mHandler.removeCallbacks(it) }
        longPressRunnable = null
      }
    }
    return true
  }

  /**
   * 为LrcView设置歌词List集合数据
   */
  override fun setLrcRows(lrcRows: List<LrcRow>?) {
    reset()

    this.lrcRows = lrcRows
    this.lrcRows?.let { lrcRows ->
      //计算每一行歌词所占的高度
//            for(LrcRow lrcRow : mLrcRows){
//                int height = 0;
//                String combine = lrcRow.getContent() + (!TextUtils.isEmpty(lrcRow.getTranslate()) ? "\t" + lrcRow.getTranslate() : "");
//                String[] multiText = combine.split("\t");
//                for (String text : multiText) {
//                    float textWidth = mPaintForOtherLrc.measureText(text);
//                    int lineNumber = (int) Math.ceil(textWidth / getWidth());
//
//                    height += lineNumber * mPaintForOtherLrc.getTextSize() + DEFAULT_SPACING_PADDING;
//                }
//                lrcRow.setHeight(height);
//            }
      calculateLrcRowHeight(lrcRows)
    }
    invalidate()
  }

  private fun calculateLrcRowHeight(lrcRows: List<LrcRow>) {
    totalHeight = 0
    for (lrcRow in lrcRows) {
      lrcRow.contentHeight = getSingleLineHeight(lrcRow.content)
      if (lrcRow.hasTranslate()) {
        lrcRow.translateHeight = getSingleLineHeight(lrcRow.translate)
      }
      lrcRow.totalHeight = lrcRow.translateHeight + lrcRow.contentHeight
      totalHeight += lrcRow.totalHeight
    }
  }

  /**
   * 获得单句歌词的高度，可能有多行
   */
  private fun getSingleLineHeight(text: String): Int {
    val staticLayout = StaticLayout(
      text, normalPaint,
      width - paddingLeft - paddingRight, Layout.Alignment.ALIGN_CENTER,
      DEFAULT_SPACING_MULTI, DEFAULT_SPACING_PADDING, true
    )
    return staticLayout.height
  }

  /**
   * 当前高亮歌词的行号
   */
  private var curRow = -1

  /**
   * 到第n行所滚动过的距离
   */
  private fun getScrollYByRow(row: Int): Int {
    if (lrcRows == null) {
      return 0
    }
    var scrollY = 0
    var i = 0

    lrcRows?.let {
      while (i < it.size && i < row) {
        scrollY += (it[i].totalHeight + linePadding).toInt()
        i++
      }
    }

    return scrollY
  }

  /**
   * 根据当前行数计算滑动距离
   */
  private val rowByScrollY: Int
    get() {
      lrcRows?.let { lrcRows ->
        var totalY = 0
        var line = 0
        while (line < lrcRows.size) {
          totalY += (linePadding + lrcRows[line].totalHeight).toInt()
          if (totalY >= scrollY) {
            return line
          }
          line++
        }
        return line - 1
      }

      return 0
    }

  private var offset = 0
  fun setOffset(offset: Int) {
    this.offset = offset
    invalidate()
  }

  override fun seekTo(p: Int, fromSeekBar: Boolean, fromSeekBarByUser: Boolean) {
    var progress = p
    if (progress != 0) {
      progress += offset
    }

    lrcRows?.let { lrcRows ->
      if (lrcRows.isEmpty()) {
        return
      }
      //如果是由seekbar的进度改变触发 并且这时候处于拖动状态，则返回
      if (fromSeekBar && canDrag) {
        return
      }
      //滑动处于等待的状态
      if (timeLineWaiting) {
        return
      }
      for (i in lrcRows.indices.reversed()) {
        if (progress >= lrcRows[i].time) {
          if (curRow != i) {
            curRow = i
            if (fromSeekBarByUser) {
              if (!scroller.isFinished) {
                scroller.forceFinished(true)
              }
              scrollTo(scrollX, getScrollYByRow(curRow))
            } else {
              smoothScrollTo(getScrollYByRow(curRow), DURATION_FOR_LRC_SCROLL)
            }
            //如果高亮歌词的宽度大于View的宽，就需要开启属性动画，让它水平滚动
//					float textWidth = mPaintForHighLightLrc.measureText(mLrcRows.get(mCurRow).getContent());
//					log("textWidth="+textWidth+"getWidth()=" + getWidth());
//					if(textWidth > getWidth()){
//						if(fromSeekBarByUser){
//							mScroller.forceFinished(true);
//						}
//						log("开始水平滚动歌词:" + mLrcRows.get(mCurRow).getContent());
//						startScrollLrc(getWidth() - textWidth, (long) (mLrcRows.get(mCurRow).getTotalTime() * 0.6));
//					}
            invalidate()
          }
          break
        }
      }
    }

  }

  /**
   * 设置歌词的缩放比例
   */
  override fun setLrcScalingFactor(newFactor: Float) {
    if (scalingFactor == newFactor) {
      return
    }
    SPUtil.putValue(
      context,
      SPUtil.LYRIC_KEY.NAME,
      SPUtil.LYRIC_KEY.LYRIC_FONT_SIZE,
      newFactor.toString()
    )
    scalingFactor = newFactor
    highLightPaint.textSize = DEFAULT_TEXT_SIZE * scalingFactor

    normalPaint.textSize = DEFAULT_TEXT_SIZE * scalingFactor

    linePadding = DEFAULT_PADDING * scalingFactor

    lrcRows?.let {
      calculateLrcRowHeight(it)
      scrollTo(scrollX, getScrollYByRow(curRow))
      scroller.forceFinished(true)
    }

    invalidate()
  }

  /**
   * 重置
   */
  override fun reset() {
    if (!scroller.isFinished) {
      scroller.forceFinished(true)
    }
    curRow = 0
    lrcRows = null
    longPressRunnable?.let { mHandler.removeCallbacks(it) }
    mHandler.removeCallbacks(timeLineDisableRunnable)
    mHandler.post(timeLineDisableRunnable)
    scrollTo(scrollX, 0)
    invalidate()
  }

  /**
   * 平滑的移动到某处
   */
  private fun smoothScrollTo(dstY: Int, duration: Int) {
    val oldScrollY = scrollY
    val offset = dstY - oldScrollY
    scroller.startScroll(scrollX, oldScrollY, scrollX, offset, duration)
    invalidate()
  }

  override fun computeScroll() {
    if (!scroller.isFinished) {
      if (scroller.computeScrollOffset()) {
        val oldY = scrollY
        val y = scroller.currY
        if (oldY != y && !canDrag) {
          scrollTo(scrollX, y)
        }
        curFraction = scroller.timePassed() * 3f / DURATION_FOR_LRC_SCROLL
        curFraction = min(curFraction, 1f)
        invalidate()
      }
    }
  }

  private var onSeekToListener: OnSeekToListener? = null
  fun setOnSeekToListener(onSeekToListener: OnSeekToListener) {
    this.onSeekToListener = onSeekToListener
  }

  fun setText(text: String) {
    this.placeholder = text
    reset()
  }

  fun setText(@StringRes res: Int) {
    this.placeholder = resources.getString(res)
    setText(placeholder)
    reset()
  }

  interface OnSeekToListener {
    fun onSeekTo(progress: Int)
  }

  private var onLrcClickListener: OnLrcClickListener? = null
  fun setOnLrcClickListener(mOnLrcClickListener: OnLrcClickListener?) {
    this.onLrcClickListener = mOnLrcClickListener
  }

  interface OnLrcClickListener {
    fun onClick()
    fun onLongClick()
  }

  fun log(o: Any?) {
    Timber.v("%s", o)
  }

  /**
   * 设置高亮歌词颜色
   */
  fun setHighLightColor(@ColorInt color: Int) {
    highLightTextColor = color
    highLightPaint.color = highLightTextColor
  }

  /**
   * 设置非高亮歌词颜色
   */
  fun setOtherColor(@ColorInt color: Int) {
    normalTextColor = color
    normalPaint.color = normalTextColor
  }

  /**
   * 设置时间线颜色
   */
  fun setTimeLineColor(@ColorInt color: Int) {
    if (timeLineTextColor != color) {
      timeLineTextColor = color
      Theme.tintDrawable(timelineDrawable, color)
      timeLinePaint.color = color
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    mHandler.removeCallbacksAndMessages(null)
  }

  companion object {
    val DEFAULT_TEXT_SIZE = DensityUtil.dip2px(App.context, 15f).toFloat()

    /**
     * 歌词间默认的行距
     */
    val DEFAULT_PADDING = DensityUtil.dip2px(App.context, 10f).toFloat()

    /**
     * 跨行歌词之间额外的行距
     */
    const val DEFAULT_SPACING_PADDING = 0f
    /** DensityUtil.dip2px(App.getContext(),5) */
    /**
     * 跨行歌词之间行距倍数
     */
    const val DEFAULT_SPACING_MULTI = 1f

    /**
     * 高亮歌词的默认字体颜色
     */
    private const val DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC = Color.BLACK

    /**
     * 其他歌词的默认字体颜色
     */
    private const val DEFAULT_COLOR_FOR_OTHER_LRC = Color.GRAY

    /**
     * 时间线默认大小
     */
    private const val DEFAULT_SIZE_FOR_TIMELINE = 35f

    /***移动一句歌词的持续时间 */
    private const val DURATION_FOR_LRC_SCROLL = 800

    /***停止触摸时 如果View需要滚动 时的持续时间 */
    private const val DURATION_FOR_ACTION_UP = 400

    /**
     * 滑动后TimeLine显示的时间
     */
    private const val DURATION_TIME_LINE = 3000

  }
}