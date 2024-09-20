package remix.myplayer.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import remix.myplayer.R
import remix.myplayer.databinding.LayoutLyricsViewBinding
import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.lyrics.PerWordLyricsLine
import remix.myplayer.theme.ThemeStore
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class LyricsView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), View.OnTouchListener {
  companion object {
    private val DEACTIVATE_DELAY = 5000.milliseconds

    private val normalTextColor
      @ColorInt get() = ThemeStore.textColorSecondary
    private val highlightTextColor
      @ColorInt get() = ThemeStore.textColorPrimary
  }

  fun interface OnSeekToListener {
    fun onSeekTo(progress: Int)
  }

  private val binding = LayoutLyricsViewBinding.inflate(LayoutInflater.from(context), this, true)

  var onSeekToListener: OnSeekToListener? = null

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (h != oldh) {
      // 给 container 上下加空白，确保第一行和最后一行歌词可以滚动到 view 中间
      binding.innerContainer.setPadding(0, h / 2, 0, h / 2)
    }
  }

  private fun newLayoutForLine(line: LyricsLine): LinearLayout {
    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    val padding = resources.getDimensionPixelSize(R.dimen.lyrics_view_lrc_block_vertical_padding)
    val textColor = normalTextColor

    val layout = LinearLayout(context)
    layout.layoutParams = params
    layout.setPadding(0, padding, 0, padding)
    layout.orientation = LinearLayout.VERTICAL
    if (line.content.isNotBlank()) {
      val view = TextView(context)
      view.layoutParams = params
      view.text = if (line is PerWordLyricsLine) {
        line.getSpannedString(0f, textColor)
      } else {
        line.content
      }
      view.setTextColor(textColor)
      layout.addView(view)
    }
    if (line.translation?.isNotBlank() == true) {
      val view = TextView(context)
      view.layoutParams = params
      view.text = line.translation
      view.setTextColor(textColor)
      layout.addView(view)
    }
    return layout
  }

  /**
   * 修改完后应立刻设置 offset
   */
  var lyrics: List<LyricsLine> = emptyList()
    @UiThread set(value) {
      if (value == field) {
        return
      }
      field = value
      binding.innerContainer.removeAllViews()
      isClickable = lyrics.isNotEmpty()
      value.forEach {
        binding.innerContainer.addView(newLayoutForLine(it))
      }
      rawProgressAndDuration = null
      lastHighlightLine = null
    }

  private var rawProgressAndDuration: Pair<Int, Int>? = null

  /**
   * 修改时自动更新 UI
   */
  var offset: Int = 0
    @UiThread set(value) {
      if (value == field) {
        return
      }
      field = value
      if (isActive) {
        showTimeIndicator()
      }
      rawProgressAndDuration?.run {
        updateProgress(first, second)
      }
    }

  private fun getTextViewOfLine(index: Int): TextView {
    check(lyrics[index].content.isNotBlank())
    val layout = binding.innerContainer.getChildAt(index) as LinearLayout
    return layout.getChildAt(0) as TextView
  }

  private fun setProgressOfLine(index: Int, progress: Float, @ColorInt color: Int) {
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
  fun updateProgress(rawProgress: Int, rawDuration: Int) {
    check(lyrics.isNotEmpty())
    rawProgressAndDuration = Pair(rawProgress, rawDuration)
    val progress = rawProgress + offset
    val duration = rawDuration + offset
    check(progress <= duration)
    val index = lyrics.binarySearchBy(progress) { it.time }.let {
      if (it < 0) -(it + 1) - 1 else it
    }
    check(index >= -1 && index < lyrics.size)
    if (index != lastHighlightLine) {
      lastHighlightLine?.let {
        setProgressOfLine(it, 0f, normalTextColor)
        lastHighlightLine = null
      }
      if (index >= 0) {
        val line = lyrics[index]
        setProgressOfLine(
          index, if (line is PerWordLyricsLine) {
            line.getProgress(
              progress, lyrics.getOrNull(index + 1)?.time ?: duration
            )
          } else 0f, highlightTextColor
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
        showTimeIndicator()
        handler.removeCallbacks(deactivateRunnable)
        handler.postDelayed(deactivateRunnable, DEACTIVATE_DELAY.inWholeMilliseconds)
      } else {
        binding.timeIndicator.visibility = View.GONE
        rawProgressAndDuration?.run {
          updateProgress(first, second)
        }
      }
    }

  private val deactivateRunnable = Runnable {
    isActive = false
  }

  @SuppressLint("SetTextI18n")
  private fun showTimeIndicator() {
    (lyrics[getNearestLine()].time - offset).coerceAtLeast(0).let {
      binding.time.text =
        "%02d:%02d.%02d".format(it / 1000 / 60, it / 1000 % 60, (it % 1000 / 10f).roundToInt())
      binding.playButton.setOnClickListener { _ ->
        onSeekToListener?.onSeekTo(it)
      }
    }
    binding.timeIndicator.visibility = View.VISIBLE
  }

  override fun onTouch(v: View, event: MotionEvent): Boolean {
    isActive = true
    return false
  }

  // 在单独函数以忽略警告
  @SuppressLint("ClickableViewAccessibility")
  private fun setupOnTouchListener() {
    binding.outerContainer.setOnTouchListener(this)
  }

  init {
    binding.outerContainer.onFlingEndListener = ResponsiveScrollView.OnFlingEndListener {
      scrollToLine(getNearestLine())
    }
    setupOnTouchListener()
  }
}
