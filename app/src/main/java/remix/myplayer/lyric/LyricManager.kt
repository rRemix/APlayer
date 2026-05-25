package remix.myplayer.lyric

import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.UiThread
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.DesktopLyricPrefs
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.data.prefs.delegate
import remix.myplayer.lyric.provider.ILyricsProvider
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.ThemeController
import remix.myplayer.ui.widget.lyric.DesktopLyricOverlay
import remix.myplayer.ui.widget.lyric.DesktopLyricUiState
import remix.myplayer.util.Util
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

// TODO https://github.com/rRemix/APlayer/issues/298
@Singleton
class LyricManager @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val themeController: ThemeController,
  val desktopLyricPrefs: DesktopLyricPrefs,
  val lyricPrefs: LyricPrefs,
  val lyricSearcher: LyricSearcher
) : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

  private val UPDATE_INTERVAL = 50.milliseconds
  private val CHECK_FOREGROUND_INTERVAL = 500.milliseconds
  private val windowManager by lazy {
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  init {
    launch(Dispatchers.Default) {
      // 每 500ms 检查一次
      var inForeground: Boolean
      while (isActive) {
        inForeground = Util.isAppOnForeground
        withContext(Dispatchers.Main) {
          isAppInForeground = inForeground
        }
        delay(CHECK_FOREGROUND_INTERVAL)
      }
    }
    launch(Dispatchers.Main) {
      currentNextLyricsLine.collect { line ->
        _desktopUiState.value = _desktopUiState.value.copy(currentLyricLine = line)
        currentLyricsLine = line.currentLine?.content ?: ""
      }
    }
    launch(Dispatchers.Main) {
      var lastSong: Song? = null
      MusicStateSource.playbackUiState.collect { state ->
        if (isPlaying != state.isPlaying) {
          isPlaying = state.isPlaying
        }
        if (state.song != lastSong) {
          lastSong = state.song
          updateLyrics(state.song, null)
        }
      }
    }
  }

  private var desktopLyricView: ComposeView? = null
  private var overlayOwner: OverlayLifeCycleOwner? = null

  private val _currentNextLyricsLine = MutableStateFlow(CurrentNextLyricsLine.SEARCHING)
  val currentNextLyricsLine = _currentNextLyricsLine.asStateFlow()

  private val _desktopUiState = MutableStateFlow(
    DesktopLyricUiState(
      false,
      desktopLyricPrefs.locked,
      CurrentNextLyricsLine.SEARCHING
    )
  )
  val desktopUiState = _desktopUiState.asStateFlow()

  var isServiceAvailable: Boolean = false
    @UiThread set(value) {
      if (field != value) {
        field = value
        ensureDesktopLyric()
      }
    }
  var isNotifyShowing: Boolean = false
    @UiThread set(value) {
      if (field != value) {
        field = value
        ensureDesktopLyric()
      }
    }
  var isScreenOn: Boolean = true
    @UiThread set(value) {
      if (field != value) {
        field = value
        ensureDesktopLyric()
      }
    }
  var isAppInForeground: Boolean = false
    @UiThread set(value) {
      if (field != value) {
        field = value
        ensureDesktopLyric()
      }
    }

  var isDesktopLyricEnabled: Boolean
    get() = lyricPrefs.desktopLyricEnabled
    @UiThread private set(value) {
      lyricPrefs.desktopLyricEnabled = value
      MessageNotifier.show(if (value) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)
      ensureDesktopLyric()
    }

  // 请求权限要 context，setter 没法多传参所以单独出来
  // activity 为 null 表示不在 Activity 里，不请求权限只 toast
  @UiThread
  fun setDesktopLyricEnabled(enabled: Boolean, activity: Activity? = null) {
    if (enabled && !XXPermissions.isGranted(context, Permission.SYSTEM_ALERT_WINDOW)) {
      if (activity != null) {
        XXPermissions.with(activity)
          .permission(Permission.SYSTEM_ALERT_WINDOW)
          .request { _, allGranted ->
            if (allGranted) {
              isDesktopLyricLocked = true
            }
          }
      }
      MessageNotifier.show(R.string.plz_give_float_permission)
      return
    }
    isDesktopLyricEnabled = enabled
  }

  var isStatusBarLyricEnabled: Boolean
    get() = lyricPrefs.statusBarLyricEnabled
    set(value) {
      lyricPrefs.statusBarLyricEnabled = value
      updateStatusBarLyric()
    }

  var isTranslationEnabled: Boolean
    get() = lyricPrefs.translationEnabled
    set(value) {
      lyricPrefs.translationEnabled = value
    }

  @UiThread
  private fun ensureDesktopLyric() {
    val shouldShow =
      isServiceAvailable && isNotifyShowing && isScreenOn && !isAppInForeground && isDesktopLyricEnabled &&
          (!isDesktopLyricLocked || isPlaying)
    if (shouldShow != (desktopLyricView != null)) {
      if (shouldShow) {
        createDesktopLyric()
      } else {
        removeDesktopLyric()
      }
    }
  }

  var isDesktopLyricLocked: Boolean
    get() = desktopLyricPrefs.locked
    @UiThread set(value) {
      MessageNotifier.show(if (value) R.string.desktop_lyric__lock_ticker else R.string.desktop_lyric__unlock)

      desktopLyricPrefs.locked = value
      _desktopUiState.value = _desktopUiState.value.copy(locked = value)

      desktopLyricView?.run {
        (layoutParams as WindowManager.LayoutParams).apply {
          applyLockState(this, value)
          windowManager.updateViewLayout(this@run, this)
        }
      }

      MusicServiceRemote.service?.run {
        updateNotification()
        updatePlaybackState()
      }

      ensureDesktopLyric()
    }

  @UiThread
  private fun createDesktopLyric() {
    check(desktopLyricView == null)

    if (!XXPermissions.isGranted(context, Permission.SYSTEM_ALERT_WINDOW)) {
      Timber.tag(TAG).w("No floating window permission, do not create")
      return
    }

    Timber.tag(TAG).v("Creating desktop lyrics")

    val param = WindowManager.LayoutParams().apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        type = WindowManager.LayoutParams.TYPE_PHONE
      }
      format = PixelFormat.RGBA_8888
      gravity = Gravity.TOP
      width = ViewGroup.LayoutParams.MATCH_PARENT
      height = ViewGroup.LayoutParams.WRAP_CONTENT
      x = 0
      y = 0
      flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      applyLockState(this, desktopLyricPrefs.locked)
    }

    val owner = OverlayLifeCycleOwner().apply {
      performRestore(null)
      handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
      handleLifecycleEvent(Lifecycle.Event.ON_START)
      handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    overlayOwner = owner

    var yPosition by desktopLyricPrefs.sp.delegate(
      "${DesktopLyricPrefs.Y_POSITION_PREFIX}${context.resources.configuration.orientation}",
      context.resources.displayMetrics.heightPixels shr 1
    )
    desktopLyricView = ComposeView(context).apply {
      setViewTreeLifecycleOwner(owner)
      setViewTreeSavedStateRegistryOwner(owner)
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
      setContent {
        val touchSlop = 1f

        DesktopLyricOverlay(
          this@LyricManager,
          themeController,
          onLock = {
            isDesktopLyricLocked = !desktopLyricPrefs.locked
          },
          onDrag = { change, amount ->
            // 更新坐标
            if (amount.absoluteValue < touchSlop) {
              return@DesktopLyricOverlay
            }
            updateDesktopPosition((layoutParams as WindowManager.LayoutParams).y + amount.roundToInt())
          },
          onDragEnd = {
            // 保存坐标
            yPosition = (layoutParams as WindowManager.LayoutParams).y
          })
      }
    }
    windowManager.addView(desktopLyricView, param)

    updateDesktopPosition(yPosition)
  }

  private fun updateDesktopPosition(newPos: Int) {
    val params = desktopLyricView?.layoutParams as WindowManager.LayoutParams
    params.y = newPos
    windowManager.updateViewLayout(desktopLyricView, params)
  }

  private fun applyLockState(params: WindowManager.LayoutParams, locked: Boolean) {
    if (locked) {
      params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        params.alpha =
          (context.getSystemService(Service.INPUT_SERVICE) as InputManager).maximumObscuringOpacityForTouch
      }
    } else {
      params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        params.alpha = 1f
      }
    }
  }

  @UiThread
  private fun removeDesktopLyric() {
    Timber.tag(TAG).v("Removing desktop lyrics")

    overlayOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    overlayOwner = null

    check(desktopLyricView != null)
    windowManager.removeView(desktopLyricView)
    desktopLyricView = null
  }

  private fun getProgressOfLine(line: LyricLine, time: Long, endTime: Long): Double {
    if (endTime <= line.time) {
      Timber.tag(TAG).w("Invalid line range, time=$time, lineTime=${line.time}, endTime=$endTime")
      return computeLineProgress(line, time, endTime)
    }

    val clampedTime = time.coerceIn(line.time, endTime)
    if (clampedTime != time) {
      Timber.tag(TAG).w("Clamped time, time=$time, lineTime=${line.time}, endTime=$endTime")
    }

    return computeLineProgress(line, time, endTime)
  }

  private fun getCurrentNextLine(
    lyrics: List<LyricLine>, offset: Long, progress: Long, duration: Long
  ): CurrentNextLyricsLine {
    if (lyrics.isEmpty()) {
      return CurrentNextLyricsLine(LyricLine.LYRICS_LINE_NO_LRC, null, null)
    }
    val progressWithOffset = progress + offset
    val index = lyrics.binarySearchBy(progressWithOffset) { it.time }.let {
      if (it < 0) -(it + 1) - 1 else it
    }
    if (index < 0) {
      check(index == -1)
      return CurrentNextLyricsLine(null, null, lyrics[0])
    }
    check(index < lyrics.size)
    val cur = lyrics[index]
    val nxt = lyrics.getOrNull(index + 1)
    return CurrentNextLyricsLine(
      cur, getProgressOfLine(cur, progressWithOffset, nxt?.time ?: (duration + offset)), nxt
    )
  }

  var lyrics: List<LyricLine>? = null
    private set(value) {
      field = value
//      launch(Dispatchers.Main) {
//        lyricsFragment?.get()?.let {
//          if (value == null) {
//            it.setLyricsSearching()
//          } else {
//            it.setLyrics(value)
//          }
//        }
//      }
    }

  var isPlaying: Boolean = false
    @UiThread set(value) {
      if (field == value) {
        return
      }
      field = value
      _desktopUiState.value = _desktopUiState.value.copy(playing = value)
      if (value) {
        launch(Dispatchers.IO) {
          updateProgress()
        }
      }
      updateStatusBarLyric()
      ensureDesktopLyric()
    }
  private var progress: Long = 0
    set(value) {
      field = value
      _currentNextLyricsLine.value = getCurrentNextLine(lyrics ?: return, offset, value, duration)
    }
  var offset: Long = 0
    @UiThread set(value) {
      field = value
      launch(Dispatchers.IO) {
        updateProgress()
        lyricSearcher.saveOffset(MusicStateSource.currentPlaybackUiState.song, value)
      }
    }
  private var duration: Long = 0

  // For status bar lyrics
  private var currentLyricsLine: String = ""
    set(value) {
      if (value == field) {
        return
      }
      field = value
      updateStatusBarLyric()
    }

  private fun updateStatusBarLyric() {
    val service = MusicServiceRemote.service ?: return
    val lyricLine = currentLyricsLine
    val shouldShow = isStatusBarLyricEnabled && isPlaying &&
        lyricLine.isNotBlank() &&
        lyricLine != LyricLine.LYRICS_LINE_NO_LRC.content

    if (shouldShow) {
      service.updateNotificationWithLrc(lyricLine)
    } else {
      service.clearStatusBarLyricNotification()
    }
  }

  private val updateMutex = Mutex()
  private var updateLyricsJob: Job? = null
  private var updateProgressJob: Job? = null

  fun updateProgress() {
    if (!updateMutex.tryLock()) {
      // 还没拿到歌词或者当前有别的线程在更新
      return
    }
    try {
//      Timber.tag(TAG).d("update progress")
      updateProgressJob?.cancel()
      val state = MusicStateSource.currentProgressState
      duration = state.duration
      progress = state.position
      if (isPlaying) {
        updateProgressJob = launch(Dispatchers.IO) {
          // TODO: should we consider thread create cost?
          delay(UPDATE_INTERVAL)
          updateProgress()
        }
      }
    } finally {
      updateMutex.unlock()
    }
  }

  fun updateLyrics(song: Song, provider: ILyricsProvider? = null): Job? {
    if (!song.valid()) {
      return null
    }
    updateProgressJob?.cancel()
    updateLyricsJob?.cancel()
    updateLyricsJob = launch(Dispatchers.IO) {
      updateMutex.withLock {
        lyrics = null
        _currentNextLyricsLine.value = CurrentNextLyricsLine.SEARCHING
        val s = lyricSearcher.getLyricsAndOffset(song, provider)
        ensureActive()
        lyrics = LrcParser.mergeTranslations(s.first, lyricPrefs.translationEnabled)
        offset = s.second
      }
      updateProgress()
    }
    return updateLyricsJob
  }

  fun clearCache(song: Song) {
    launch(Dispatchers.IO) {
      lyricSearcher.clearCache(song)
    }
  }

  fun clearAllCache(includePersistent: Boolean = false) {
    launch(Dispatchers.IO) {
      lyricSearcher.clearAllCache(includePersistent)
    }
  }

  companion object {

    private const val TAG = "LyricsManager"

    /**
     * 计算当前行的播放进度：普通歌词返回 0~1，逐字歌词返回 0~words.size
     *
     * @param line 当前歌词行
     * @param time 当前播放时间
     * @param endTime 当前行的结束时间
     */
    fun computeLineProgress(line: LyricLine, time: Long, endTime: Long): Double {
      if (endTime <= line.time) {
        return when (line) {
          is PerWordLyricLine -> if (time > line.time) line.words.size.toDouble() else 0.0
          else -> if (time > line.time) 1.0 else 0.0
        }
      }

      val clampedTime = time.coerceIn(line.time, endTime)
      return if (line is PerWordLyricLine) {
        line.getProgress(clampedTime, endTime)
      } else {
        (clampedTime - line.time).toDouble() / (endTime - line.time)
      }
    }

    const val ACTION_LYRIC = "action_lyric"

    const val EXTRA_LYRIC = "extra_lyric"
    const val EXTRA_LYRIC_URI = "extra_lyric_uri"

    const val CHANGE_LYRIC = 1
    const val CHANGE_LYRIC_FONT_SCALE = 2
    const val SHOW_OFFSET_PANEL = 3
  }
}

private class OverlayLifeCycleOwner : LifecycleOwner, SavedStateRegistryOwner {

  private val lifeCycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle
    get() = lifeCycleRegistry
  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  fun setCurrentState(state: Lifecycle.State) {
    lifeCycleRegistry.currentState = state
  }

  fun handleLifecycleEvent(event: Lifecycle.Event) {
    lifeCycleRegistry.handleLifecycleEvent(event)
  }

  fun performRestore(savedState: Bundle?) {
    savedStateRegistryController.performRestore(savedState)
  }

  fun performSave(outBundle: Bundle) {
    savedStateRegistryController.performSave(outBundle)
  }

}
