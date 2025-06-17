package remix.myplayer.lyrics

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.UiThread
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.lyrics.provider.ILyricsProvider
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.LockScreenActivity
import remix.myplayer.ui.fragment.LyricsFragment
import remix.myplayer.ui.widget.desktop.DesktopLyricsView
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.DESKTOP_LYRICS_KEY
import remix.myplayer.util.SPUtil.LYRICS_KEY
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.milliseconds

object LyricsManager : CoroutineScope by CoroutineScope(Dispatchers.IO) {
  private const val TAG = "LyricsManager"
  private val UPDATE_INTERVAL = 50.milliseconds

  private val windowManager by lazy {
    App.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  private var desktopLyricsView: DesktopLyricsView? = null

  private var lyricsFragment: WeakReference<LyricsFragment>? = null
  private var lockScreenActivity: WeakReference<LockScreenActivity>? = null

  fun setLyricsFragment(fragment: LyricsFragment) {
    lyricsFragment = WeakReference(fragment)
    lyrics?.let {
      fragment.setLyrics(it)
    } ?: fragment.setLyricsSearching()
    fragment.setProgress(progress, duration)
    fragment.setOffset(offset)
  }

  fun setLockScreenActivity(activity: LockScreenActivity) {
    lockScreenActivity = WeakReference(activity)
    currentNextLyricsLine = currentNextLyricsLine
  }

  var isServiceAvailable: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyrics()
    }
  var isNotifyShowing: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyrics()
    }
  var isScreenOn: Boolean = true
    @UiThread set(value) {
      field = value
      ensureDesktopLyrics()
    }
  var isAppInForeground: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyrics()
    }

  var isDesktopLyricsEnabled: Boolean
    get() = SPUtil.getValue(App.context, LYRICS_KEY.NAME, LYRICS_KEY.DESKTOP_LYRICS_ENABLED, false)
    @UiThread private set(value) {
      SPUtil.putValue(
        App.context, LYRICS_KEY.NAME, LYRICS_KEY.DESKTOP_LYRICS_ENABLED, value
      )
      ToastUtil.show(
        App.context, if (value) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc
      )
      ensureDesktopLyrics()
    }

  // 请求权限要 context，setter 没法多传参所以单独出来
  // activity 为 null 表示不在 Activity 里，不请求权限只 toast
  @UiThread
  fun setDesktopLyricsEnabled(enabled: Boolean, activity: Activity? = null) {
    if (enabled && !XXPermissions.isGranted(App.context, Permission.SYSTEM_ALERT_WINDOW)) {
      if (activity != null) {
        XXPermissions.with(activity)
            .permission(Permission.SYSTEM_ALERT_WINDOW)
            .request { _, allGranted ->
              if (allGranted) {
                isDesktopLyricsLocked = true
              }
            }
      }
      ToastUtil.show(App.context, R.string.plz_give_float_permission)
      return
    }
    isDesktopLyricsEnabled = enabled
  }

  var isStatusBarLyricsEnabled: Boolean
    get() = SPUtil.getValue(
      App.context, LYRICS_KEY.NAME, LYRICS_KEY.STATUS_BAR_LYRICS_ENABLED, false
    )
    set(value) {
      SPUtil.putValue(
        App.context, LYRICS_KEY.NAME, LYRICS_KEY.STATUS_BAR_LYRICS_ENABLED, value
      )
      // TODO: remove existing (but how?)
    }

  @UiThread
  private fun ensureDesktopLyrics() {
    val shouldShow =
      isServiceAvailable && isNotifyShowing && isScreenOn && !isAppInForeground && isDesktopLyricsEnabled
    if (shouldShow != (desktopLyricsView != null)) {
      if (shouldShow) {
        createDesktopLyrics()
      } else {
        removeDesktopLyrics()
      }
    }
  }

  var isDesktopLyricsLocked: Boolean
    get() = desktopLyricsView?.isLocked ?: false
    @UiThread set(value) {
      desktopLyricsView?.run {
        isLocked = value
      } ?: {
        // 没有桌面歌词时自己动设置
        SPUtil.putValue(
          App.context, DESKTOP_LYRICS_KEY.NAME, DESKTOP_LYRICS_KEY.LOCKED, value
        )
      }
    }

  @UiThread
  private fun createDesktopLyrics() {
    check(desktopLyricsView == null)

    if (!XXPermissions.isGranted(App.context, Permission.SYSTEM_ALERT_WINDOW)) {
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
    }

    desktopLyricsView = DesktopLyricsView(ContextThemeWrapper(App.context, ThemeStore.themeRes))
    windowManager.addView(desktopLyricsView, param)
    desktopLyricsView!!.restoreWindowPosition()
    desktopLyricsView!!.isPlaying = isPlaying
    desktopLyricsView!!.setLyrics(currentNextLyricsLine)
  }

  @UiThread
  private fun removeDesktopLyrics() {
    Timber.v("Removing desktop lyrics")
    check(desktopLyricsView != null)
    windowManager.removeView(desktopLyricsView)
    desktopLyricsView = null
  }

  private fun getProgressOfLine(line: LyricsLine, time: Long, endTime: Long): Double {
    require(time in line.time..endTime)
    return if (line is PerWordLyricsLine) {
      line.getProgress(time, endTime)
    } else {
      (time - line.time).toDouble() / (endTime - line.time)
    }
  }

  private fun getCurrentNextLine(
    lyrics: List<LyricsLine>, offset: Long, progress: Long, duration: Long
  ): CurrentNextLyricsLine {
    if (lyrics.isEmpty()) {
      return CurrentNextLyricsLine(LyricsLine.LYRICS_LINE_NO_LRC, null, null)
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

  private var lyrics: List<LyricsLine>? = null
    set(value) {
      field = value
      launch(Dispatchers.Main) {
        lyricsFragment?.get()?.let {
          if (value == null) {
            it.setLyricsSearching()
          } else {
            it.setLyrics(value)
          }
        }
      }
    }

  var isPlaying: Boolean = false
    @UiThread set(value) {
      field = value
      desktopLyricsView?.isPlaying = value
      if (value) {
        launch(Dispatchers.IO) {
          updateProgress()
        }
      }
    }
  private var progress: Long = 0
    set(value) {
      field = value
      launch(Dispatchers.Main) {
        lyricsFragment?.get()?.setProgress(value, duration)
      }
      currentNextLyricsLine = getCurrentNextLine(lyrics ?: return, offset, value, duration)
    }
  var offset: Long = 0
    @UiThread set(value) {
      field = value
      lyricsFragment?.get()?.setOffset(offset)
      launch(Dispatchers.IO) {
        updateProgress()
        LyricsSearcher.saveOffset(MusicServiceRemote.getCurrentSong(), value)
      }
    }
  private var duration: Long = 0

  private var currentNextLyricsLine: CurrentNextLyricsLine = CurrentNextLyricsLine.SEARCHING
    set(value) {
      if (value != field) {
        field = value
        currentLyricsLine = value.currentLine?.content ?: ""
        launch(Dispatchers.Main) {
          lockScreenActivity?.get()?.setLyrics(value)
          desktopLyricsView?.setLyrics(value)
        }
      }
    }

  // For status bar lyrics
  private var currentLyricsLine: String = ""
    set(value) {
      if (value != field && isStatusBarLyricsEnabled) {
        MusicServiceRemote.service?.run {
          field = value
          updateNotificationWithLrc(value)
        }
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
      progress = MusicServiceRemote.getProgress().toLong()
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

  fun updateLyrics(song: Song, provider: ILyricsProvider? = null) {
    updateLyricsJob?.cancel()
    updateLyricsJob = launch(Dispatchers.IO) {
      updateMutex.withLock {
        lyrics = null
        currentNextLyricsLine = CurrentNextLyricsLine.SEARCHING
        val s = LyricsSearcher.getLyricsAndOffset(song, provider)
        ensureActive()
        duration = song.duration
        lyrics = s.first
        offset = s.second
      }
      updateProgress()
    }
  }
}
