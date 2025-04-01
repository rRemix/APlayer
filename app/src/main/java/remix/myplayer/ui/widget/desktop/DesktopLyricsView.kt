package remix.myplayer.ui.widget.desktop

import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import remix.myplayer.R
import remix.myplayer.databinding.LayoutDesktopLyricsBinding
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.lyrics.CurrentNextLyricsLine
import remix.myplayer.lyrics.LyricsManager
import remix.myplayer.service.Command
import remix.myplayer.theme.MaterialTintHelper
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.DESKTOP_LYRICS_KEY
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@UiThread
class DesktopLyricsView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
  companion object {
    private const val TAG = "DesktopLyricsView"

    private const val ELLIPSIS = Typography.ellipsis.toString()

    private val HIDE_PANEL_DELAY = 3000.milliseconds

    private const val DEFAULT_FIRST_LINE_SIZE = 18f
    private const val DEFAULT_SECOND_LINE_SIZE = 16f

    @ColorInt
    private const val DEFAULT_SUNG_COLOR = 0xff698cf6.toInt()

    @ColorInt
    private const val DEFAULT_UNSUNG_COLOR = 0xffd4d4d4.toInt()

    @ColorInt
    private const val DEFAULT_TRANSLATION_COLOR = 0xffd4d4d4.toInt()
  }

  private val binding = LayoutDesktopLyricsBinding.inflate(LayoutInflater.from(context), this, true)

  private val windowManager by lazy {
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }
  private val inputManager by lazy {
    context.getSystemService(Service.INPUT_SERVICE) as InputManager
  }

  private var secondLineIsTranslation: Boolean = false

  private var yPosition: Int
    get() = SPUtil.getValue(
      context,
      DESKTOP_LYRICS_KEY.NAME,
      DESKTOP_LYRICS_KEY.Y_POSITION_PREFIX + resources.configuration.orientation,
      0
    )
    set(value) = SPUtil.putValue(
      context,
      DESKTOP_LYRICS_KEY.NAME,
      DESKTOP_LYRICS_KEY.Y_POSITION_PREFIX + resources.configuration.orientation,
      value
    )
  private var firstLineSize: Float
    get() = SPUtil.getValue(
      context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.FIRST_LINE_SIZE, DEFAULT_FIRST_LINE_SIZE
    )
    set(v) {
      binding.linesContainer.firstLine.setTextSize(COMPLEX_UNIT_SP, v)
      SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.FIRST_LINE_SIZE, v)
      // TODO("fix window position?")
    }
  private var secondLineSize: Float
    get() = SPUtil.getValue(
      context,
      DESKTOP_LYRICS_KEY.NAME,
      DESKTOP_LYRICS_KEY.SECOND_LINE_SIZE,
      DEFAULT_SECOND_LINE_SIZE
    )
    set(v) {
      binding.linesContainer.secondLine.setTextSize(COMPLEX_UNIT_SP, v)
      SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.SECOND_LINE_SIZE, v)
      // TODO("fix window position?")
    }

  var sungColor: Int
    @ColorInt get() = SPUtil.getValue(
      context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.SUNG_COLOR, DEFAULT_SUNG_COLOR
    )
    set(@ColorInt v) {
      SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.SUNG_COLOR, v)
      binding.linesContainer.firstLine.sungColor = v
    }
  var unsungColor: Int
    @ColorInt get() = SPUtil.getValue(
      context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.UNSUNG_COLOR, DEFAULT_UNSUNG_COLOR
    )
    set(@ColorInt v) {
      SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.UNSUNG_COLOR, v)
      binding.linesContainer.firstLine.unsungColor = v
      if (!secondLineIsTranslation) {
        binding.linesContainer.secondLine.setTextColor(v)
      }
    }
  var translationColor: Int
    @ColorInt get() = SPUtil.getValue(
      context,
      DESKTOP_LYRICS_KEY.NAME,
      DESKTOP_LYRICS_KEY.TRANSLATION_COLOR,
      DEFAULT_TRANSLATION_COLOR
    )
    set(@ColorInt v) {
      SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.TRANSLATION_COLOR, v)
      if (secondLineIsTranslation) {
        binding.linesContainer.secondLine.setTextColor(v)
      }
    }

  var isPlaying: Boolean = true // 初值与 xml 对应；由 service 负责修改
    set(value) {
      if (value != field) {
        field = value
        binding.playPause.setImageResource(
          if (value) R.drawable.ic_pause_black_24dp
          else R.drawable.ic_play_arrow_black_24dp
        )
        ImageViewCompat.setImageTintList(
          binding.playPause,
          AppCompatResources.getColorStateList(context, R.color.desktop_lyrics_control_color)
        )
      }
    }

  fun setLyrics(content: CurrentNextLyricsLine) {
    binding.linesContainer.firstLine.lyricsLine = content.currentLine
    binding.linesContainer.firstLine.progress = content.currentLineProgress
    if (!content.currentLine?.translation.isNullOrBlank()) {
      setTranslation(content.currentLine?.translation!!)
    } else {
      // 翻译和下一行歌词都没有时显示省略号，统一用显示下一行歌词的颜色
      setNextLine((content.nextLine?.content ?: "").ifBlank { ELLIPSIS })
    }
  }

  private fun setTranslation(translation: String) {
    if (!secondLineIsTranslation) {
      secondLineIsTranslation = true
      binding.linesContainer.secondLine.setTextColor(translationColor)
    }
    if (translation != binding.linesContainer.secondLine.text) {
      binding.linesContainer.secondLine.text = translation
    }
  }

  private fun setNextLine(content: String) {
    if (secondLineIsTranslation) {
      secondLineIsTranslation = false
      binding.linesContainer.secondLine.setTextColor(unsungColor)
    }
    if (content != binding.linesContainer.secondLine.text) {
      binding.linesContainer.secondLine.text = content
    }
  }

  private var isPanelVisible: Boolean = true // 初值与 xml 对应
    set(value) {
      if (value != field) {
        Timber.tag(TAG).v("set isPanelVisible: $value")
        field = value
        binding.root.setBackgroundColor(
          ResourcesCompat.getColor(
            resources,
            if (value) R.color.desktop_lyrics_window_background else R.color.transparent,
            null
          )
        )
        binding.root.children.forEach {
          if (it.id != R.id.lines_container) {
            it.visibility = if (value) View.VISIBLE else View.GONE
          }
        }
        if (value) {
          // 控制组件由隐藏转为显示时，字体颜色和大小设置默认隐藏
          isSettingsVisible = false
        }
      }
      if (value) {
        handler.run {
          removeCallbacks(hidePanelRunnable)
          postDelayed(hidePanelRunnable, HIDE_PANEL_DELAY.inWholeMilliseconds)
        }
      }
    }

  private val hidePanelRunnable = Runnable {
    isPanelVisible = false
  }

  private var isSettingsVisible: Boolean = true // 初值与 xml 对应
    set(value) {
      arrayOf(binding.divider, binding.settingsContainer).forEach {
        it.visibility = if (value) View.VISIBLE else View.GONE
      }
      field = value
    }

  var isLocked: Boolean
    get() = SPUtil.getValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.LOCKED, false)
    set(value) {
      (layoutParams as WindowManager.LayoutParams?)?.run {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (value) {
          flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alpha = inputManager.maximumObscuringOpacityForTouch
          }
        }
        windowManager.updateViewLayout(this@DesktopLyricsView, this)
      }
      if (isLocked != value) {
        SPUtil.putValue(context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.LOCKED, value)
        MusicServiceRemote.service?.run {
          updateNotification()
          updatePlaybackState()
        }
        ToastUtil.show(
          context, if (value) R.string.desktop_lyric_lock else R.string.desktop_lyric__unlock
        )
      }
    }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    binding.linesContainer.firstLine.setTextSize(COMPLEX_UNIT_SP, firstLineSize)
    binding.linesContainer.secondLine.setTextSize(COMPLEX_UNIT_SP, secondLineSize)
    binding.linesContainer.firstLine.sungColor = sungColor
    binding.linesContainer.firstLine.unsungColor = unsungColor

    setLyrics(CurrentNextLyricsLine(null, null, null))

    isLocked = isLocked

    isPanelVisible = false

    binding.close.setOnClickListener {
      LyricsManager.setDesktopLyricsEnabled(false)
    }
    binding.prev.setOnClickListener {
      sendLocalBroadcast(makeCmdIntent(Command.PREV))
      isPanelVisible = true
    }
    binding.playPause.setOnClickListener {
      sendLocalBroadcast(makeCmdIntent(Command.TOGGLE))
      isPanelVisible = true
    }
    binding.next.setOnClickListener {
      sendLocalBroadcast(makeCmdIntent(Command.NEXT))
      isPanelVisible = true
    }
    binding.lock.setOnClickListener {
      isPanelVisible = false
      isLocked = true
      ToastUtil.show(context, R.string.desktop_lyric_lock)
    }
    binding.settings.setOnClickListener {
      isSettingsVisible = !isSettingsVisible
      isPanelVisible = true
    }
    binding.colorSettings.setOnClickListener {
      handler?.removeCallbacks(hidePanelRunnable)
      // TODO: Set colors
      isPanelVisible = true // 设置完回来触发自动隐藏？
    }
    MaterialTintHelper.setTint(binding.firstLineSizeSlider)
    MaterialTintHelper.setTint(binding.secondLineSizeSlider)
    binding.firstLineSizeSlider.setLabelFormatter { "${it}sp" }
    binding.secondLineSizeSlider.setLabelFormatter { "${it}sp" }
    binding.firstLineSizeSlider.value = firstLineSize
    binding.secondLineSizeSlider.value = secondLineSize
    // TODO: Change on stop? onStopTrackingTouch
    binding.firstLineSizeSlider.addOnChangeListener { _, value, _ ->
      isPanelVisible = true
      firstLineSize = value
    }
    binding.secondLineSizeSlider.addOnChangeListener { _, value, _ ->
      isPanelVisible = true
      secondLineSize = value
    }
  }

  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var isTouching = false
  private var isDragging = false
  private var lastPointerY: Float? = null
  private var lastWindowY: Int? = null

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (isLocked) {
      Timber.tag(TAG).w("isLocked is true but received touch event: $event")
      return false
    }
    if (event.actionMasked != MotionEvent.ACTION_DOWN && !isTouching) {
      // 会收到全屏幕的点击事件，但只有 ACTION_DOWN 在 View 上的是需要我们处理的
      return false
    }
    val params = layoutParams as WindowManager.LayoutParams
    return when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
//        Timber.tag(TAG).d("onTouchEvent ACTION_DOWN ${event.y} $top $bottom $height")
        if (event.y < top || event.y > bottom) {
          isTouching = false
          false
        } else {
          handler?.removeCallbacks(hidePanelRunnable)
          isTouching = true
          isDragging = false
          lastPointerY = event.rawY
          lastWindowY = params.y
          true
        }
      }

      MotionEvent.ACTION_MOVE -> {
        if (abs(event.rawY - lastPointerY!!) >= touchSlop) {
          isDragging = true
        }
        if (isDragging) {
          params.y = lastWindowY!! + (event.rawY - lastPointerY!!).roundToInt()
          windowManager.updateViewLayout(this, params)
        }
        true
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        isTouching = false
        if (isDragging) {
          params.y = lastWindowY!! + (event.rawY - lastPointerY!!).roundToInt()
          windowManager.updateViewLayout(this, params)
          isPanelVisible = isPanelVisible // 触发自动隐藏
          isDragging = false
          lastPointerY = null
          lastWindowY = null
          saveWindowLocation()
        } else {
          if (event.action == MotionEvent.ACTION_UP) {
            // 避免警告
            performClick()
          }
        }
        true
      }

      else -> false
    }
  }

  override fun performClick(): Boolean {
    if (super.performClick()) {
      return true
    }
    isPanelVisible = true
    return true
  }

  private fun saveWindowLocation() {
    yPosition = (layoutParams as WindowManager.LayoutParams).y
  }

  fun restoreWindowPosition() {
    val params = layoutParams as WindowManager.LayoutParams
    params.y = yPosition
    windowManager.updateViewLayout(this, params)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // TODO: Does it work?
    Timber.tag(TAG).v("onConfigurationChanged, new orientation: ${newConfig.orientation}")
    restoreWindowPosition()
  }
}
