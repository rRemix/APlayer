package remix.myplayer.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import remix.myplayer.databinding.LayoutLyricsLineBinding
import remix.myplayer.databinding.LayoutLyricsViewBinding
import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.lyrics.PerWordLyricsLine
import remix.myplayer.theme.ThemeStore
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

class LyricsView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), View.OnTouchListener {
  companion object {
    private const val TAG = "LyricsView"

    private val DEACTIVATE_DELAY = 5000.milliseconds
    private val AUTO_SCROLL_DELAY = 200.milliseconds

    private val normalTextColor
      @ColorInt get() = ThemeStore.textColorSecondary
    private val highlightTextColor
      @ColorInt get() = ThemeStore.textColorPrimary
  }

  fun interface OnSeekToListener {
    fun onSeekTo(progress: Long)
  }

  private val binding = LayoutLyricsViewBinding.inflate(LayoutInflater.from(context), this, true)

  var onSeekToListener: OnSeekToListener? = null

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    Timber.tag(TAG).v("onSizeChanged, h=$h")
    if (h != oldh) {
      // 给 container 上下加空白，确保第一行和最后一行歌词可以滚动到 view 中间
      val padding = (h + 1) / 2
      handler.post {
        binding.innerContainer.setPadding(0, padding, 0, padding)
        handler.post { // 要等一次 layout
          if (!isActive) {
            scrollToLine(lastHighlightLine)
          }
        }
      }
    }
  }

  private fun addLayoutForLine(line: LyricsLine) {
    val layout =
      LayoutLyricsLineBinding.inflate(LayoutInflater.from(context), binding.innerContainer, true)
    if (line.content.isNotBlank()) {
      layout.content.text = if (line is PerWordLyricsLine) {
        line.getSpannedString(0.0, normalTextColor)
      } else {
        line.content
      }
    }
    if (!line.translation.isNullOrBlank()) {
      layout.translation.text = line.translation
    }
  }

  /**
   * 修改完后应立刻设置 offset
   */
  var lyrics: List<LyricsLine> = emptyList()
    @UiThread set(value) {
      field = value
      binding.innerContainer.removeAllViews()
      isClickable = value.isNotEmpty()
      value.forEach {
        addLayoutForLine(it)
      }
      rawProgressAndDuration = null
      lastHighlightLine = null
      isActive = false
    }

  private data class ProgressAndDuration(val progress: Long, val duration: Long)
  private var rawProgressAndDuration: ProgressAndDuration? = null

  /**
   * 修改时自动更新 UI
   */
  var offset: Long = 0
    @UiThread set(value) {
      if (value == field) {
        return
      }
      field = value
      if (isActive) {
        updateTimeIndicator()
      }
      rawProgressAndDuration?.run {
        setProgress(progress, duration)
      }
    }

  private fun getTextViewOfLine(index: Int): TextView {
    val layout = binding.innerContainer.getChildAt(index) as LinearLayout
    return layout.getChildAt(0) as TextView
  }

  private fun setProgressOfLine(index: Int, progress: Double, @ColorInt color: Int) {
    val view = getTextViewOfLine(index)
    val line = lyrics[index]
    if (line.content.isBlank()) {
      return
    }
    if (line is PerWordLyricsLine) {
      view.text = line.getSpannedString(progress, color)
    } else {
      view.setTextColor(color)
    }
  }

  private var lastHighlightLine: Int? = null

  @UiThread
  fun setProgress(rawProgress: Long, rawDuration: Long) {
    check(lyrics.isNotEmpty())
    rawProgressAndDuration = ProgressAndDuration(rawProgress, rawDuration)
    val progress = rawProgress + offset
    val duration = rawDuration + offset
    check(progress <= duration)
    val index = lyrics.binarySearchBy(progress) { it.time }.let {
      if (it < 0) -(it + 1) - 1 else it
    }
    check(index >= -1 && index < lyrics.size)
    if (index != lastHighlightLine) {
      lastHighlightLine?.let {
        setProgressOfLine(it, 0.0, normalTextColor)
        lastHighlightLine = null
      }
      if (index >= 0) {
        val line = lyrics[index]
        setProgressOfLine(
          index, if (line is PerWordLyricsLine) {
            line.getProgress(
              progress, lyrics.getOrNull(index + 1)?.time ?: duration
            )
          } else 0.0, highlightTextColor
        )
        lastHighlightLine = index
      }
      if (!isActive) {
        scrollToLine(index)
      }
    }
  }

  private fun getNearestLine(): Int {
    val y = binding.outerContainer.scrollY + binding.outerContainer.height / 2f
    var line: Int = -1
    var minDistance = Float.POSITIVE_INFINITY
    for (i in lyrics.indices) {
      val view = binding.innerContainer.getChildAt(i)
      if (y >= view.top && y <= view.bottom) {
        return i
      }
      val distance = if (y < view.top) (view.top - y) else (y - view.bottom)
      if (distance < minDistance) {
        line = i
        minDistance = distance
      }
    }
    check(line != -1)
    return line
  }

  private fun scrollToLine(line: Int?) {
    val y = if (line == null || line < 0) {
      0
    } else {
      val view = binding.innerContainer.getChildAt(line)
      ((view.top + view.bottom - binding.outerContainer.height) / 2f).roundToInt()
    }
    binding.outerContainer.smoothScrollTo(0, y)
  }

  private var isActive: Boolean = false
    set(value) {
      field = value
      if (value) {
        updateTimeIndicator()
        binding.timeIndicator.visibility = View.VISIBLE
        handler.removeCallbacks(deactivateRunnable)
        handler.postDelayed(deactivateRunnable, DEACTIVATE_DELAY.inWholeMilliseconds)
      } else {
        binding.timeIndicator.visibility = View.GONE
        rawProgressAndDuration?.run {
          setProgress(progress, duration)
        }
      }
    }

  private val deactivateRunnable = Runnable {
    isActive = false
    scrollToLine(lastHighlightLine)
  }
  private val scrollToNearestLineRunnable = Runnable {
    scrollToLine(getNearestLine())
  }

  @SuppressLint("SetTextI18n")
  private fun updateTimeIndicator() {
    (lyrics[getNearestLine()].time - offset).coerceIn(0, rawProgressAndDuration?.duration ?: 0).let {
      val time = (it / 10.0).roundToLong()
      binding.time.text = "%02d:%02d.%02d".format(time / 100 / 60, time / 100 % 60, time % 100)
      binding.playButton.setOnClickListener { _ ->
        onSeekToListener?.onSeekTo(it)
      }
    }
  }

  private var isTouching: Boolean = false

  override fun onTouch(v: View, event: MotionEvent): Boolean {
    check(v == binding.outerContainer)

    isActive = true

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        isTouching = true
        handler.removeCallbacks(scrollToNearestLineRunnable)
      }

      MotionEvent.ACTION_UP -> {
        isTouching = false
        handler.postDelayed(scrollToNearestLineRunnable, AUTO_SCROLL_DELAY.inWholeMilliseconds)
      }
    }

    return false
  }

  private fun onScrollChange() {
    updateTimeIndicator()

    handler.removeCallbacks(scrollToNearestLineRunnable)
    if (!isTouching) {
      handler.postDelayed(scrollToNearestLineRunnable, AUTO_SCROLL_DELAY.inWholeMilliseconds)
    }
  }

  // 在单独函数以忽略警告
  @SuppressLint("ClickableViewAccessibility")
  private fun init() {
    binding.outerContainer.setOnTouchListener(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      binding.outerContainer.setOnScrollChangeListener { _, _, _, _, _ ->
        onScrollChange()
      }
    } else {
      binding.outerContainer.viewTreeObserver.addOnScrollChangedListener {
        onScrollChange()
      }
    }
  }

  init {
    init()
  }
}
