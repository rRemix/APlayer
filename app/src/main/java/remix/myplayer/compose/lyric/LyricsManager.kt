package remix.myplayer.compose.lyric

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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.provider.ILyricsProvider
import remix.myplayer.compose.prefs.LyricPrefs
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.LockScreenActivity
import remix.myplayer.ui.widget.desktop.DesktopLyricView
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsManagerEntryPoint {

  fun lyricsManager(): LyricsManager
}

// TODO https://github.com/rRemix/APlayer/issues/298
@Singleton
class LyricsManager @Inject constructor(
  @ApplicationContext
  private val context: Context,
  val lyricPrefs: LyricPrefs,
  val lyricSearcher: LyricSearcher
) : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

  private val UPDATE_INTERVAL = 50.milliseconds
  private val CHECK_FOREGROUND_INTERVAL = 500.milliseconds
  private val windowManager by lazy {
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  init {
    launch(Dispatchers.Main) {
      // 每秒检查一次
      while (isActive) {
        delay(CHECK_FOREGROUND_INTERVAL)
        isAppInForeground = Util.isAppOnForeground
      }
    }
  }

  private var desktopLyricView: DesktopLyricView? = null

  //  private var lyricsFragment: WeakReference<LyricsFragment>? = null
  private var lockScreenActivity: WeakReference<LockScreenActivity>? = null

//  fun setLyricsFragment(fragment: LyricsFragment) {
//    lyricsFragment = WeakReference(fragment)
//    lyrics?.let {
//      fragment.setLyrics(it)
//    } ?: fragment.setLyricsSearching()
//    fragment.setProgress(progress, duration)
//    fragment.setOffset(offset)
//  }

  fun setLockScreenActivity(activity: LockScreenActivity) {
    lockScreenActivity = WeakReference(activity)
    currentNextLyricsLine = currentNextLyricsLine
  }

  fun clearLockScreenActivity() {
    lockScreenActivity?.clear()
    lockScreenActivity = null
  }

  var isServiceAvailable: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyric()
    }
  var isNotifyShowing: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyric()
    }
  var isScreenOn: Boolean = true
    @UiThread set(value) {
      field = value
      ensureDesktopLyric()
    }
  var isAppInForeground: Boolean = false
    @UiThread set(value) {
      field = value
      ensureDesktopLyric()
    }

  var isDesktopLyricEnabled: Boolean
    get() = lyricPrefs.desktopLyricEnabled
    @UiThread private set(value) {
      lyricPrefs.desktopLyricEnabled = value
      ToastUtil.show(
        context, if (value) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc
      )
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
      ToastUtil.show(context, R.string.plz_give_float_permission)
      return
    }
    isDesktopLyricEnabled = enabled
  }

  var isStatusBarLyricEnabled: Boolean
    get() = lyricPrefs.statusBarLyricEnabled
    set(value) {
      lyricPrefs.statusBarLyricEnabled = value
      // TODO: remove existing (but how?)
    }

  @UiThread
  private fun ensureDesktopLyric() {
    val shouldShow =
      isServiceAvailable && isNotifyShowing && isScreenOn && !isAppInForeground && isDesktopLyricEnabled
    if (shouldShow != (desktopLyricView != null)) {
      if (shouldShow) {
        createDesktopLyric()
      } else {
        removeDesktopLyric()
      }
    }
  }

  var isDesktopLyricLocked: Boolean
    get() = desktopLyricView?.isLocked == true
    @UiThread set(value) {
      desktopLyricView?.run {
        isLocked = value
      } ?: {
        // 没有桌面歌词时自己动设置
        lyricPrefs.desktopLyricLocked = value
      }
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
    }

    desktopLyricView = DesktopLyricView(ContextThemeWrapper(context, ThemeStore.themeRes))
    windowManager.addView(desktopLyricView, param)
    desktopLyricView!!.restoreWindowPosition()
    desktopLyricView!!.isPlaying = isPlaying
    desktopLyricView!!.setLyrics(currentNextLyricsLine)
  }

  @UiThread
  private fun removeDesktopLyric() {
    Timber.v("Removing desktop lyrics")
    check(desktopLyricView != null)
    windowManager.removeView(desktopLyricView)
    desktopLyricView = null
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

  var lyrics: List<LyricsLine>? = null
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
      field = value
      desktopLyricView?.isPlaying = value
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
//        lyricsFragment?.get()?.setProgress(value, duration)
      }
      currentNextLyricsLine = getCurrentNextLine(lyrics ?: return, offset, value, duration)
    }
  var offset: Long = 0
    @UiThread set(value) {
      field = value
//      lyricsFragment?.get()?.setOffset(offset)
      launch(Dispatchers.IO) {
        updateProgress()
        // TODO
        lyricSearcher.saveOffset(MusicServiceRemote.getCurrentSong(), value)
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
          desktopLyricView?.setLyrics(value)
        }
      }
    }

  // For status bar lyrics
  private var currentLyricsLine: String = ""
    set(value) {
      if (value != field && isStatusBarLyricEnabled) {
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

  fun updateLyrics(song: Song, provider: ILyricsProvider? = null): Job? {
    updateProgressJob?.cancel()
    updateLyricsJob?.cancel()
    updateLyricsJob = launch(Dispatchers.IO) {
      updateMutex.withLock {
        lyrics = null
        currentNextLyricsLine = CurrentNextLyricsLine.SEARCHING
        val s = lyricSearcher.getLyricsAndOffset(song, provider)
        ensureActive()
        duration = song.duration
        lyrics = s.first
        offset = s.second
      }
      updateProgress()
    }
    return updateLyricsJob
  }

  fun clearCache(song: Song) {
    lyricSearcher.clearCache(song)
  }

  companion object {

    private const val TAG = "LyricsManager"

    const val ACTION_LYRIC = "action_lyric"

    const val EXTRA_LYRIC = "extra_lyric"
    const val EXTRA_LYRIC_URI = "LyricUri"

    const val CHANGE_LYRIC = 1
    const val CHANGE_LYRIC_FONT_SCALE = 2
    const val SHOW_OFFSET_PANEL = 3
  }
}
