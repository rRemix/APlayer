package remix.myplayer.ui.activity

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.palette.graphics.Palette
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lockscreen.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.lyric.LyricFetcher
import remix.myplayer.lyric.LyricFetcher.Companion.LYRIC_FIND_INTERVAL
import remix.myplayer.lyric.bean.LyricRowWrapper
import remix.myplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_NO
import remix.myplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_SEARCHING
import remix.myplayer.misc.menu.CtrlButtonListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.blur.StackBlurManager
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
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

  //是否正在播放
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
  private val IMAGE_SIZE by lazy {
    DensityUtil.dip2px(this, 210f)
  }
  private val BLUR_SIZE by lazy {
    DensityUtil.dip2px(this, 100f)
  }
  private val CONFIG by lazy {
    RequestConfig.Builder(BLUR_SIZE, BLUR_SIZE).build()
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
    } catch (e: Exception){
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
    if (disposable != null) {
      disposable?.dispose()
    }
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
    LibraryUriRequest(lockscreen_image,
        getSearchRequestWithAlbumType(song),
        RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load()

    if (disposable != null) {
      disposable?.dispose()
    }
    disposable = object : ImageUriRequest<Palette>(CONFIG) {
      override fun onError(throwable: Throwable) {
//        ToastUtil.show(mContext, throwable.message)
      }

      override fun onSuccess(result: Palette?) {
        setResult(result)
      }

      override fun load(): Disposable {
        return getThumbBitmapObservable(getSearchRequestWithAlbumType(song))
            .compose(RxUtil.applySchedulerToIO())
            .flatMap { bitmap ->
              Observable.create<Palette> { e ->
                processBitmap(e, bitmap)
              }
            }
            .onErrorResumeNext(Observable.create { processBitmap(it, DEFAULT_BITMAP) })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ this.onSuccess(it) }, { this.onError(it) })
      }
    }.load()
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

  private fun processBitmap(e: ObservableEmitter<Palette>, raw: Bitmap?) {
    if (isFinishing) {
      e.onComplete()
      return
    }
    rawBitMap = MusicService.copy(raw)
    if (rawBitMap == null || rawBitMap?.isRecycled == true) {
      e.onComplete()
      return
    }
    val stackBlurManager = StackBlurManager(rawBitMap)
    blurBitMap = stackBlurManager.processNatively(40)
    val palette = Palette.from(rawBitMap ?: return).generate()
    e.onNext(palette)
    e.onComplete()
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
    private var songInThread = Song.EMPTY_SONG

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
