package remix.myplayer.ui.activity

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_lockscreen.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.glide.GlideApp
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.lyric.LyricFetcher
import remix.myplayer.lyric.LyricFetcher.Companion.LYRIC_FIND_INTERVAL
import remix.myplayer.lyric.bean.LyricRowWrapper
import remix.myplayer.lyric.bean.LyricRowWrapper.Companion.LYRIC_WRAPPER_NO
import remix.myplayer.lyric.bean.LyricRowWrapper.Companion.LYRIC_WRAPPER_SEARCHING
import remix.myplayer.misc.menu.CtrlButtonListener
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.blur.StackBlurManager
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.RxUtil
import remix.myplayer.util.StatusBarUtil
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by Remix on 2016/3/9.
 */

/**
 * 锁屏界面
 */

class LockScreenActivity : BaseMusicActivity() {
  //高斯模糊后的bitmap
  private var blurBitMap: Bitmap? = null

  //高斯模糊之前的bitmap
  private var rawBitMap: Bitmap? = null
  private var width: Int = 0

  private var disposable: Disposable? = null

  @Volatile
  private var curLyric: LyricRowWrapper? = null
  private var updateLyricThread: UpdateLockScreenLyricThread? = null

  //前后两次触摸的X
  private var scrollX1: Float = 0f
  private var scrollX2: Float = 0f

  //一次移动的距离
  private var distance: Float = 0f

  private val DEFAULT_BITMAP by lazy {
    BitmapFactory.decodeResource(resources, R.drawable.album_empty_bg_night)
  }

  override fun setUpTheme() {}

  override fun setStatusBarColor() {
    StatusBarUtil.setTransparent(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lockscreen)
    try {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    } catch (e: Exception) {
      Timber.v(e)
    }

    val metric = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metric)
    width = metric.widthPixels

    //解锁屏幕
    val attr = window.attributes
    attr.flags = attr.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
    attr.flags = attr.flags or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

    //初始化按钮
    val listener = CtrlButtonListener(applicationContext)
    lockscreen_prev.setOnClickListener(listener)
    lockscreen_next.setOnClickListener(listener)
    lockscreen_play.setOnClickListener(listener)

    //初始化控件
    lockscreen_background.alpha = 0.75f
    window.decorView.setBackgroundColor(Color.TRANSPARENT)

    findViewById<View>(R.id.lockscreen_arrow_container)
        .startAnimation(AnimationUtils.loadAnimation(this, R.anim.arrow_left_to_right))

  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val decorView = window.decorView ?: return true
    when (event.action) {
      MotionEvent.ACTION_DOWN -> scrollX1 = event.x
      MotionEvent.ACTION_MOVE -> {
        scrollX2 = event.x
        distance = scrollX2 - scrollX1
        scrollX1 = scrollX2
        //如果往右或者是往左没有超过最左边,移动View
        if (distance > 0 || decorView.scrollX + -distance < 0) {
          decorView.scrollBy((-distance).toInt(), 0)
        }
      }
      MotionEvent.ACTION_UP -> {
        //判断当前位置是否超过整个屏幕宽度的0.25
        //超过则finish;没有则移动回初始状态
        if (-decorView.scrollX > width * 0.25) {
          finish()
        } else {
          decorView.scrollTo(0, 0)
        }
        scrollX1 = 0f
        distance = scrollX1
      }
    }
    return true
  }

  override fun onStart() {
    super.onStart()
    overridePendingTransition(0, 0)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, R.anim.cover_right_out)
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onDestroy() {
    super.onDestroy()
    if (updateLyricThread != null) {
      updateLyricThread?.interrupt()
      updateLyricThread = null
    }
    disposable?.dispose()
    disposable = null
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    onMetaChanged()
    onPlayStateChange()
  }


  override fun onMetaChanged() {
    super.onMetaChanged()
    val song = MusicServiceRemote.getCurrentSong()
    //歌词
    if (updateLyricThread == null) {
      val service = MusicServiceRemote.service
      if (service != null) {
        updateLyricThread = UpdateLockScreenLyricThread(this, service)
        updateLyricThread?.start()
      }
    }

    //标题
    lockscreen_song.text = song.title
    //艺术家
    lockscreen_artist.text = song.artist
    //封面
    GlideApp.with(this)
        .asBitmap()
        .load(song)
        .centerCrop()
        .dontAnimate()
        .placeholder(R.drawable.album_empty_bg_night)
        .error(R.drawable.album_empty_bg_night)
        .addListener(object : RequestListener<Bitmap> {
          override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            startProcess(DEFAULT_BITMAP)
            return false
          }

          override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            startProcess(resource)
            return false
          }

        })
        .into(iv)
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    //更新播放按钮

    lockscreen_play.setImageResource(
        if (MusicServiceRemote.isPlaying()) R.drawable.lock_btn_pause else R.drawable.lock_btn_play)
  }

  private fun setResult(result: Palette?) {
    if (result == null) {
      return
    }
    lockscreen_background.setImageBitmap(blurBitMap)

    val swatch = ColorUtil.getSwatch(result)
    lockscreen_song.setTextColor(swatch.bodyTextColor)
    lockscreen_artist.setTextColor(swatch.titleTextColor)
    lockscreen_lyric.setTextColor(swatch.bodyTextColor)
  }

  private fun startProcess(resource: Bitmap?) {
    disposable?.dispose()
    disposable = Single
        .fromCallable {
          blurBitmap(resource ?: DEFAULT_BITMAP)
        }
        .compose(RxUtil.applySingleScheduler())
        .subscribe(Consumer {
          setResult(it)
        })
  }

  private fun blurBitmap(raw: Bitmap): Palette? {
    if (isFinishing) {
      return null
    }

    rawBitMap = MusicService.copy(raw)
    if (rawBitMap == null || rawBitMap?.isRecycled == true) {
      return null
    }

    val stackBlurManager = StackBlurManager(rawBitMap)
    blurBitMap = stackBlurManager.processNatively(40)
    return Palette.from(rawBitMap ?: return null).generate()
  }

  private fun setCurrentLyric(wrapper: LyricRowWrapper) {
    runOnUiThread {
      curLyric = wrapper
      if (curLyric == null || curLyric === LYRIC_WRAPPER_NO) {
        lockscreen_lyric.setTextWithAnimation(R.string.no_lrc)
      } else if (curLyric === LYRIC_WRAPPER_SEARCHING) {
        lockscreen_lyric.text = ""
      } else {
        lockscreen_lyric.setTextWithAnimation(
            String.format("%s\n%s", curLyric?.lineOne?.content,
                curLyric?.lineTwo?.content))
      }

    }
  }

  private class UpdateLockScreenLyricThread constructor(activity: LockScreenActivity, service: MusicService) : Thread() {

    private val ref: WeakReference<LockScreenActivity> = WeakReference(activity)
    private val lyricFetcher: LyricFetcher = LyricFetcher(service)
    private var songInThread: Song = Song.EMPTY_SONG

    override fun interrupt() {
      super.interrupt()
      lyricFetcher.dispose()
    }

    override fun run() {
      while (true) {
        try {
          sleep(LYRIC_FIND_INTERVAL)
        } catch (e: InterruptedException) {
          return
        }

        val song = MusicServiceRemote.getCurrentSong()
        if (songInThread !== song) {
          songInThread = song
          lyricFetcher.updateLyricRows(songInThread)
          continue
        }

        val activity = ref.get()
        activity?.setCurrentLyric(lyricFetcher.findCurrentLyric())
      }
    }
  }

}
