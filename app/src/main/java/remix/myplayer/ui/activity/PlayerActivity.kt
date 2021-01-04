package remix.myplayer.ui.activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.layout_player_control.*
import kotlinx.android.synthetic.main.layout_player_topbar.*
import kotlinx.android.synthetic.main.layout_player_volume.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.getDuration
import remix.myplayer.helper.MusicServiceRemote.getNextSong
import remix.myplayer.helper.MusicServiceRemote.getOperation
import remix.myplayer.helper.MusicServiceRemote.getPlayModel
import remix.myplayer.helper.MusicServiceRemote.getProgress
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.helper.MusicServiceRemote.setPlayModel
import remix.myplayer.lyric.LrcView
import remix.myplayer.lyric.LrcView.OnLrcClickListener
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnInflateFinishListener
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.misc.menu.AudioPopupListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.theme.DrawableGradient
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.adapter.PagerAdapter
import remix.myplayer.ui.dialog.FileChooserDialog
import remix.myplayer.ui.dialog.FileChooserDialog.FileCallback
import remix.myplayer.ui.dialog.PlayQueueDialog
import remix.myplayer.ui.dialog.PlayQueueDialog.Companion.newInstance
import remix.myplayer.ui.fragment.CoverFragment
import remix.myplayer.ui.fragment.LyricFragment
import remix.myplayer.ui.fragment.RecordFragment
import remix.myplayer.util.*
import remix.myplayer.util.SPUtil.SETTING_KEY
import timber.log.Timber
import java.io.File
import java.lang.Exception
import kotlin.math.abs

/**
 * Created by Remix on 2015/12/1.
 */
/**
 * 播放界面
 */
class PlayerActivity : BaseMusicActivity(), FileCallback {
  private var valueAnimator: ValueAnimator? = null

  //上次选中的Fragment
  private var prevPosition = 0

  //第一次启动的标志变量
  private var firstStart = true

  //是否正在拖动进度条
  var isDragSeekBarFromUser = false

  //歌词控件
  private var lrcView: LrcView? = null

  //高亮与非高亮指示器
  private lateinit var highLightIndicator: GradientDrawable
  private lateinit var normalIndicator: GradientDrawable
  private val indicators: ArrayList<ImageView> = arrayListOf()

  //当前播放的歌曲
  private lateinit var song: Song

  //当前是否播放
  private var isPlaying = false

  //当前播放时间
  private var currentTime = 0

  //当前歌曲总时长
  private var duration = 0

  //Fragment
  lateinit var lyricFragment: LyricFragment
    private set
  private lateinit var coverFragment: CoverFragment

  /**
   * 下拉关闭
   */
  private var eventY1 = 0f
  private var eventY2 = 0f
  private var eventX1 = 0f
  private var eventX2 = 0f

  /**
   * 更新Handler
   */
  private val handler: MsgHandler by lazy {
    MsgHandler(this)
  }

  /**
   * 更新封面与背景的Handler
   */
  private val audioManager: AudioManager by lazy {
    getSystemService(AUDIO_SERVICE) as AudioManager
  }

  //底部显示控制
  private var bottomConfig = 0
  private val volumeRunnable = Runnable {
    next_song.startAnimation(makeAnimation(next_song, true))
    volume_container.startAnimation(makeAnimation(volume_container, false))
  }
  private val receiver: Receiver = Receiver()

  private val background by lazy {
    SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAYER_BACKGROUND, BACKGROUND_THEME)
  }

  override fun setUpTheme() {
//    if (ThemeStore.isLightTheme()) {
//      super.setUpTheme();
//    } else {
//      setTheme(R.style.AudioHolderStyle_Night);
//    }
    val superThemeRes = ThemeStore.getThemeRes()
    val themeRes: Int
    themeRes = when (superThemeRes) {
      R.style.Theme_APlayer_Black -> R.style.PlayerActivityStyle_Black
      R.style.Theme_APlayer_Dark -> R.style.PlayerActivityStyle_Dark
      else -> R.style.PlayerActivityStyle
    }
    setTheme(themeRes)
  }

  override fun setNavigationBarColor() {
    super.setNavigationBarColor()
    //导航栏变色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && ThemeStore.sColoredNavigation) {
      val navigationColor = ThemeStore.getBackgroundColorMain(this)
      window.navigationBarColor = navigationColor
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor))
    }
  }

  override fun setStatusBarMode() {
    StatusBarUtil.setStatusBarMode(this, ThemeStore.getBackgroundColorMain(this))
  }

  override fun setStatusBarColor() {
    when (background) {
      BACKGROUND_THEME -> {
        StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getBackgroundColorMain(this))
      }
      BACKGROUND_ADAPTIVE_COLOR -> {
        StatusBarUtil.setTransparent(this)
      }
      BACKGROUND_CUSTOM_IMAGE -> {
        StatusBarUtil.setTransparent(this)

        val file = File(DiskCache.getDiskCacheDir(this, "thumbnail/player"), "player.jpg");
        if (file.exists()) {
          val bitmap = BitmapFactory.decodeFile(file.absolutePath)
          player_container.background = BitmapDrawable(resources, bitmap)
          updateSwatch(bitmap)
        }
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_player)
    ButterKnife.bind(this)

    song = getCurrentSong()
    if (song == Song.EMPTY_SONG && intent.hasExtra(EXTRA_SONG)) {
      song = intent.getParcelableExtra(EXTRA_SONG) as Song
    }

    setUpBottom()
    setUpTop()
    setUpFragments()
    setUpIndicator()
    setUpSeekBar()
    setUpViewColor()
    Util.registerLocalReceiver(receiver, IntentFilter(ACTION_UPDATE_NEXT))
  }

  public override fun onResume() {
    super.onResume()
    if (this.isPortraitOrientation()) {
      view_pager.currentItem = 0
    }
    //更新进度条
    ProgressThread().start()
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    onMetaChanged()
    onPlayStateChange()
  }

  override fun onStart() {
    super.onStart()
    overridePendingTransition(R.anim.audio_in, 0)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, R.anim.audio_out)
  }

  /**
   * 上一首 下一首 播放、暂停
   */
  @OnClick(R.id.playbar_next, R.id.playbar_prev, R.id.playbar_play_container)
  fun onCtrlClick(v: View) {
    val intent = Intent(MusicService.ACTION_CMD)
    when (v.id) {
      R.id.playbar_prev -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.PREV)
      R.id.playbar_next -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.NEXT)
      R.id.playbar_play_container -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.TOGGLE)
    }
    Util.sendLocalBroadcast(intent)
  }

  /**
   * 播放模式 播放列表 关闭 隐藏
   */
  @OnClick(R.id.playbar_model, R.id.playbar_playinglist, R.id.top_hide, R.id.top_more)
  fun onOtherClick(v: View) {
    when (v.id) {
      R.id.playbar_model -> {
        var currentModel = getPlayModel()
        currentModel = if (currentModel == Constants.MODE_REPEAT) Constants.MODE_LOOP else ++currentModel
        setPlayModel(currentModel)
        playbar_model.setImageDrawable(Theme.tintDrawable(when (currentModel) {
          Constants.MODE_LOOP -> R.drawable.play_btn_loop
          Constants.MODE_SHUFFLE -> R.drawable.play_btn_shuffle
          else -> R.drawable.play_btn_loop_one
        }, ThemeStore.getPlayerBtnColor()))
        val msg = if (currentModel == Constants.MODE_LOOP) getString(R.string.model_normal) else if (currentModel == Constants.MODE_SHUFFLE) getString(R.string.model_random) else getString(R.string.model_repeat)
        //刷新下一首
        if (currentModel != Constants.MODE_SHUFFLE) {
          next_song.text = getString(R.string.next_song, getNextSong().title)
        }
        ToastUtil.show(this, msg)
      }
      R.id.playbar_playinglist -> newInstance()
          .show(supportFragmentManager, PlayQueueDialog::class.java.simpleName)
      R.id.top_hide -> onBackPressed()
      R.id.top_more -> {
        @SuppressLint("RestrictedApi") val popupMenu = PopupMenu(mContext, v, Gravity.TOP)
        popupMenu.menuInflater.inflate(R.menu.menu_audio_item, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(AudioPopupListener<PlayerActivity>(this, song))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          popupMenu.menu.removeItem(R.id.menu_speed)
        }
        popupMenu.show()
      }
    }
  }

  @SuppressLint("CheckResult")
  @OnClick(R.id.volume_down, R.id.volume_up, R.id.next_song)
  fun onVolumeClick(view: View) {
    when (view.id) {
      R.id.volume_down -> Completable
          .fromAction {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_PLAY_SOUND)
          }
          .subscribeOn(Schedulers.io())
          .subscribe()
      R.id.volume_up -> Completable
          .fromAction {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND)
          }
          .subscribeOn(Schedulers.io())
          .subscribe()
      R.id.next_song -> if (bottomConfig == BOTTOM_SHOW_BOTH) {
        next_song.startAnimation(makeAnimation(next_song, false))
        volume_container.startAnimation(makeAnimation(volume_container, true))
        handler.removeCallbacks(volumeRunnable)
        handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
      }
    }
    if (view.id != R.id.next_song) {
      Single.zip(Single.fromCallable { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) },
          Single.fromCallable { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) },
          BiFunction { max: Int, current: Int -> longArrayOf(max.toLong(), current.toLong()) })
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { longs: LongArray -> volume_seekbar.progress = (longs[1] * 1.0 / longs[1] * 100).toInt() }
    }
  }

  private fun makeAnimation(view: View, show: Boolean): AlphaAnimation {
    val alphaAnimation = AlphaAnimation(if (show) 0f else 1f, if (show) 1f else 0f)
    alphaAnimation.duration = 300
    alphaAnimation.setAnimationListener(object : AnimationListener {
      override fun onAnimationStart(animation: Animation) {
        View.VISIBLE.also { view.visibility = it }
      }

      override fun onAnimationEnd(animation: Animation) {
        (if (show) View.VISIBLE else View.INVISIBLE).also { view.visibility = it }
      }

      override fun onAnimationRepeat(animation: Animation) {}
    })
    return alphaAnimation
  }

  /**
   * 初始化三个dot
   */
  private fun setUpIndicator() {
    val width = DensityUtil.dip2px(this, 8f)
    val height = DensityUtil.dip2px(this, 2f)
    highLightIndicator = GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(ThemeStore.getAccentColor())
        .make()
    normalIndicator = GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(ThemeStore.getAccentColor())
        .alpha(0.3f)
        .make()
    indicators.add(findViewById(R.id.guide_01))
    indicators.add(findViewById(R.id.guide_02))
    indicators[0].setImageDrawable(highLightIndicator)
    indicators[1].setImageDrawable(normalIndicator)
  }

  /**
   * 初始化seekbar
   */
  @SuppressLint("CheckResult")
  private fun setUpSeekBar() {

    //初始化已播放时间与剩余时间
    duration = song.getDuration().toInt()
    val temp = getProgress()
    currentTime = if (temp in 1 until duration) temp else 0
    if (duration > 0 && duration - currentTime > 0) {
      text_hasplay.text = Util.getTime(currentTime.toLong())
      text_remain.text = Util.getTime((duration - currentTime).toLong())
    }

    //初始化seekbar
    if (duration > 0 && duration < Int.MAX_VALUE) {
      seekbar.max = duration
    } else {
      seekbar.max = 1000
    }
    if (currentTime in 1 until duration) {
      seekbar.progress = currentTime
    } else {
      seekbar.progress = 0
    }
    seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
          updateProgressText(progress)
        }
        handler.sendEmptyMessage(UPDATE_TIME_ONLY)
        currentTime = progress
        lrcView?.seekTo(progress, true, fromUser)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
        isDragSeekBarFromUser = true
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        //没有播放拖动进度条无效
//                if(!mIsPlay){
//                    seekBar.setProgress(0);
//                }
        MusicServiceRemote.setProgress(seekBar.progress)
        isDragSeekBarFromUser = false
      }
    })

    //音量的Seekbar
    Single.zip(Single.fromCallable { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) },
        Single.fromCallable { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) },
        BiFunction { max: Int, current: Int -> intArrayOf(max, current) })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { ints: IntArray ->
          val current = ints[1]
          val max = ints[0]
          volume_seekbar.progress = (current * 1.0 / max * 100).toInt()
          volume_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
              if (bottomConfig == BOTTOM_SHOW_BOTH) {
                handler.removeCallbacks(volumeRunnable)
                handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
              }
              if (fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    (seekBar.progress / 100f * max).toInt(),
                    AudioManager.FLAG_PLAY_SOUND)
              }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
          })
        }
    if (bottomConfig == BOTTOM_SHOW_BOTH) {
      handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
    }
  }

  /**
   * 更新顶部歌曲信息
   */
  private fun updateTopStatus(song: Song?) {
    if (song == null) {
      return
    }
    val title = song.title ?: ""
    val artist = song.artist ?: ""
    val album = song.album ?: ""
    if (title == "") {
      top_title.text = getString(R.string.unknown_song)
    } else {
      top_title.text = title
    }
    when {
      artist == "" -> {
        top_detail.text = song.album
      }
      album == "" -> {
        top_detail.text = song.artist
      }
      else -> {
        top_detail.text = String.format("%s-%s", song.artist, song.album)
      }
    }
  }

  /**
   * 更新播放、暂停按钮
   */
  private fun updatePlayButton(isPlay: Boolean) {
    isPlaying = isPlay
    playbar_play_pause.updateState(isPlay, true)
  }

  /**
   * 初始化顶部信息
   */
  private fun setUpTop() {
    updateTopStatus(song)
  }

  /**
   * 初始化viewpager
   */
  @SuppressLint("ClickableViewAccessibility")
  private fun setUpFragments() {
    val fragmentManager = supportFragmentManager
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    fragmentManager.executePendingTransactions()
    val fragments = fragmentManager.fragments

    for (fragment in fragments) {
      if (fragment is LyricFragment ||
          fragment is CoverFragment ||
          fragment is RecordFragment) {
        fragmentManager.beginTransaction().remove(fragment).commitNow()
      }
    }
    coverFragment = CoverFragment()
    setUpCoverFragment()
    lyricFragment = LyricFragment()
    setUpLyricFragment()

    if (this.isPortraitOrientation()) {
//      mRecordFragment = new RecordFragment();

      //Viewpager
      val adapter = PagerAdapter(supportFragmentManager)
      //      adapter.addFragment(mRecordFragment);
      adapter.addFragment(coverFragment)
      adapter.addFragment(lyricFragment)
      view_pager.adapter = adapter
      view_pager.offscreenPageLimit = adapter.count - 1
      view_pager.currentItem = 0
      val thresholdY = DensityUtil.dip2px(mContext, 40f)
      val thresholdX = DensityUtil.dip2px(mContext, 60f)
      //下滑关闭
      view_pager.setOnTouchListener { v: View?, event: MotionEvent ->
        if (event.action == MotionEvent.ACTION_DOWN) {
          eventX1 = event.x
          eventY1 = event.y
        }
        if (event.action == MotionEvent.ACTION_UP) {
          eventX2 = event.x
          eventY2 = event.y
          if (eventY2 - eventY1 > thresholdY && abs(eventX1 - eventX2) < thresholdX) {
            onBackPressed()
          }
        }
        false
      }
      view_pager.addOnPageChangeListener(object : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
          indicators[prevPosition].setImageDrawable(normalIndicator)
          indicators[position].setImageDrawable(highLightIndicator)
          prevPosition = position
          //歌词界面常亮
          if (position == 1 && SPUtil
                  .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SCREEN_ALWAYS_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
        }

        override fun onPageScrollStateChanged(state: Int) {}
      })
    } else {
      fragmentManager
          .beginTransaction()
          .replace(R.id.container_cover, coverFragment)
          .replace(R.id.container_lyric, lyricFragment)
          .commit()
    }

    //歌词界面常亮
    if (SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SCREEN_ALWAYS_ON,
            false)) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  private fun setUpLyricFragment() {
    lyricFragment.setOnInflateFinishListener(OnInflateFinishListener { view: View? ->
      lrcView = view as LrcView
      lrcView?.setOnLrcClickListener(object : OnLrcClickListener {
        override fun onClick() {}
        override fun onLongClick() {}
      })
      lrcView?.setOnSeekToListener { progress: Int ->
        if (progress > 0 && progress < getDuration()) {
          MusicServiceRemote.setProgress(progress)
          currentTime = progress
          handler.sendEmptyMessage(UPDATE_TIME_ALL)
        }
      }
      lrcView?.setHighLightColor(ThemeStore.getTextColorPrimary())
      lrcView?.setOtherColor(ThemeStore.getTextColorSecondary())
      lrcView?.setTimeLineColor(ThemeStore.getTextColorSecondary())
    })
  }

  private fun setUpCoverFragment() {
    coverFragment.coverCallback = object : CoverFragment.CoverCallback {
      override fun onBitmap(bitmap: Bitmap?) {
        if (background == BACKGROUND_ADAPTIVE_COLOR) {
          updateSwatch(bitmap)
        }
      }
    }
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    val newSong = getCurrentSong()
    updateTopStatus(newSong)
    lyricFragment.updateLrc(newSong)
    song = newSong
    coverFragment.setImage(song, false, true)
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    song = getCurrentSong()
    //当操作不为播放或者暂停且正在运行时，更新所有控件
    val operation = getOperation()
    if (operation != Command.TOGGLE || firstStart) {
      //更新顶部信息
      updateTopStatus(song)
      //更新歌词
      handler.postDelayed({ lyricFragment.updateLrc(song) }, 500)
      //更新进度条
      val temp = getProgress()
      currentTime = if (temp in 1 until duration) temp else 0
      duration = song.getDuration().toInt()
      seekbar.max = duration
      //更新下一首歌曲
      next_song.text = getString(R.string.next_song, getNextSong().title)
      coverFragment.setImage(song,
          operation != Command.TOGGLE && !firstStart,
          operation != Command.TOGGLE)
      firstStart = false
    }
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    //更新按钮状态
    val isPlay = isPlaying()
    if (isPlaying != isPlay) {
      updatePlayButton(isPlay)
    }
  }

  //更新进度条线程
  private inner class ProgressThread : Thread() {
    override fun run() {
      while (mIsForeground) {
        try {
          //音量
          if (volume_seekbar.visibility == View.VISIBLE) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            runOnUiThread { volume_seekbar.progress = (current * 1.0 / max * 100).toInt() }
          }
          if (!isPlaying()) {
            sleep(500)
            continue
          }
          val progress = getProgress()
          if (progress in 1 until duration) {
            currentTime = progress
            handler.sendEmptyMessage(UPDATE_TIME_ALL)
            sleep(500)
          }
        } catch (ignore: Exception){
        }
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return super.onKeyDown(keyCode, event)
  }

  /**
   * 初始化底部区域
   */
  private fun setUpBottom() {
    bottomConfig = SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN, BOTTOM_SHOW_BOTH)
    if (!this.isPortraitOrientation()) { //横屏不显示底部
      bottomConfig = BOTTOM_SHOW_NONE
    }
    when (bottomConfig) {
      BOTTOM_SHOW_NEXT -> { //仅显示下一首
        volume_container.visibility = View.GONE
        next_song.visibility = View.VISIBLE
      }
      BOTTOM_SHOW_VOLUME -> { //仅显示音量控制
        volume_container.visibility = View.VISIBLE
        next_song.visibility = View.GONE
      }
      BOTTOM_SHOW_NONE -> { //关闭
        val volumeLayout = findViewById<View>(R.id.layout_player_volume)
        volumeLayout.visibility = View.INVISIBLE
        val volumeLp = volumeLayout.layoutParams as LinearLayout.LayoutParams
        volumeLp.weight = 0f
        volumeLayout.layoutParams = volumeLp
        val controlLayout = findViewById<View>(R.id.layout_player_control)
        val controlLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0)
        controlLp.weight = 2f
        controlLayout.layoutParams = controlLp
      }
    }
  }

  /**
   * 根据主题颜色修改按钮颜色
   */
  private fun setUpViewColor() {
    val accentColor = ThemeStore.getAccentColor()
    val tintColor = ThemeStore.getPlayerBtnColor()
    updateSeekBarColor(accentColor)
    //        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(context,10),DensityUtil.dip2px(context,10)));
//        Drawable seekbarBackground = mProgressSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

    //修改控制按钮颜色
    Theme.tintDrawable(playbar_next, R.drawable.play_btn_next, accentColor)
    Theme.tintDrawable(playbar_prev, R.drawable.play_btn_pre, accentColor)
    playbar_play_pause.setBackgroundColor(accentColor)

    //歌曲名颜色
    top_title.setTextColor(ThemeStore.getPlayerTitleColor())

    //修改顶部按钮颜色
    Theme.tintDrawable(top_hide, R.drawable.icon_player_back, tintColor)
    Theme.tintDrawable(top_more, R.drawable.icon_player_more, tintColor)
    //播放模式与播放队列
    val playMode = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL,
        Constants.MODE_LOOP)
    Theme.tintDrawable(playbar_model, if (playMode == Constants.MODE_LOOP) R.drawable.play_btn_loop else if (playMode == Constants.MODE_SHUFFLE) R.drawable.play_btn_shuffle else R.drawable.play_btn_loop_one, tintColor)
    Theme.tintDrawable(playbar_playinglist, R.drawable.play_btn_normal_list, tintColor)

    //音量控制
    volume_down.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
    volume_up.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
    //        Theme.tintDrawable(mVolumeDown,R.drawable.ic_volume_down_black_24dp,tintColor);
//        Theme.tintDrawable(mVolumeUp,R.drawable.ic_volume_up_black_24dp,tintColor);
    //下一首背景
    next_song.background = GradientDrawableMaker()
        .color(ThemeStore.getPlayerNextSongBgColor())
        .corner(DensityUtil.dip2px(2f).toFloat())
        .width(DensityUtil.dip2px(288f))
        .height(DensityUtil.dip2px(38f))
        .make()
    next_song.setTextColor(ThemeStore.getPlayerNextSongTextColor())
  }

  private fun updateSeekBarColor(color: Int) {
    setProgressDrawable(seekbar, color)
    setProgressDrawable(volume_seekbar, color)

    //修改thumb
    val inset = DensityUtil.dip2px(mContext, 6f)
    val width = DensityUtil.dip2px(this, 2f)
    val height = DensityUtil.dip2px(this, 6f)
    seekbar.thumb = InsetDrawable(
        GradientDrawableMaker()
            .width(width)
            .height(height)
            .color(color)
            .make(),
        inset, inset, inset, inset)
    volume_seekbar.thumb = InsetDrawable(GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(color)
        .make(),
        inset, inset, inset, inset)
  }

  private fun setProgressDrawable(seekBar: SeekBar, color: Int) {
    val progressDrawable = seekBar.progressDrawable as LayerDrawable
    //修改progress颜色
    (progressDrawable.getDrawable(0) as GradientDrawable).setColor(ThemeStore.getPlayerProgressColor())
    progressDrawable.getDrawable(1).setColorFilter(color, PorterDuff.Mode.SRC_IN)
    seekBar.progressDrawable = progressDrawable
  }

  private fun updateBackground(uri: Uri) {
    object : ImageUriRequest<Bitmap>(RequestConfig.Builder(100, 100).build()) {
      override fun onError(throwable: Throwable?) {
        Timber.v("updateBackground failed: $throwable")
        updateSwatch(null)
      }

      override fun onSuccess(result: Bitmap?) {
        updateSwatch(result)
      }

      override fun load(): Disposable {
        return getThumbBitmapObservable(uri)
            .compose(RxUtil.applyScheduler())
            .subscribe({
              onSuccess(it)
            }, {
              onError(it)
            })
      }
    }.load()

  }

  @SuppressLint("CheckResult")
  private fun updateSwatch(bitmap: Bitmap?) {
    Single
        .fromCallable {
          bitmap
        }
        .map { result: Bitmap ->
          val palette = Palette.from(result).generate()
          if (palette.mutedSwatch != null) {
            return@map palette.mutedSwatch
          }
          val swatches = ArrayList<Swatch>(palette.swatches)
          swatches.sortWith(Comparator { o1, o2 -> o1.population.compareTo(o2.population) })

          return@map if (swatches.isNotEmpty()) swatches[0] else Swatch(Color.GRAY, 100);
        }
        .onErrorReturnItem(Swatch(Color.GRAY, 100))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ swatch: Swatch? ->
          if (swatch == null) {
            return@subscribe
          }

          updateViewsColorBySwatch(swatch)
          startBGColorAnimation(swatch)

        }) { t: Throwable? -> Timber.v(t) }
  }

  private fun updateViewsColorBySwatch(swatch: Swatch) {
    playbar_next.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
    playbar_prev.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
    playbar_play_pause.setBackgroundColor(swatch.rgb)

    volume_down.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
    volume_up.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

    playbar_model.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
    playbar_playinglist.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

    normalIndicator.setColor(ColorUtil.adjustAlpha(swatch.rgb, 0.3f))
    highLightIndicator.setColor(swatch.rgb)

    updateSeekBarColor(swatch.rgb)
    next_song.setBackgroundColor(ColorUtil.adjustAlpha(swatch.rgb, 0.1f))

    top_title.setTextColor(swatch.titleTextColor)
    top_detail.setTextColor(swatch.bodyTextColor)

    top_more.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
    top_hide.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
  }

  private fun startBGColorAnimation(swatch: Swatch) {
    valueAnimator?.cancel()

    valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), Theme.resolveColor(this, R.attr.colorSurface), swatch.rgb)

    valueAnimator?.addUpdateListener { animation ->
      val drawable = DrawableGradient(GradientDrawable.Orientation.TOP_BOTTOM,
          intArrayOf(animation.animatedValue as Int,
              Theme.resolveColor(this, R.attr.colorSurface)), 0)
      player_container.background = drawable
    }
    valueAnimator?.setDuration(1000)?.start()
  }

  override fun onDestroy() {
    super.onDestroy()
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    handler.remove()
    Util.unregisterLocalReceiver(receiver)
  }

  /**
   * 选择歌词文件
   */
  override fun onFileSelection(dialog: FileChooserDialog, file: File) {
//        //如果之前忽略过该歌曲的歌词，取消忽略
//        Set<String> ignoreLrcId = new HashSet<>(SPUtil.getStringSet(this,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID"));
//        if(ignoreLrcId.size() > 0){
//            for (String id : ignoreLrcId){
//                if((mInfo.getID() + "").equals(id)){
//                    ignoreLrcId.remove(mInfo.getID() + "");
//                    SPUtil.putStringSet(context,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID",ignoreLrcId);
//                }
//            }
//        }
    SPUtil.putValue(mContext, SPUtil.LYRIC_KEY.NAME, song.id.toString() + "",
        SPUtil.LYRIC_KEY.LYRIC_MANUAL)
    lyricFragment.updateLrc(file.absolutePath)
    Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
  }

  override fun onFileChooserDismissed(dialog: FileChooserDialog) {}

  private fun updateProgressText(current: Int) {
    if (current > 0 && duration - current > 0) {
      text_hasplay.text = Util.getTime(current.toLong())
      text_remain.text = Util.getTime((duration - current).toLong())
    }
  }

  private fun updateProgressByHandler() {
    updateProgressText(currentTime)
  }

  private fun updateSeekBarByHandler() {
    seekbar.progress = currentTime
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
//        if(msg.what == UPDATE_BG){
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        }
    if (msg.what == UPDATE_TIME_ONLY && !isDragSeekBarFromUser) {
      updateProgressByHandler()
    }
    if (msg.what == UPDATE_TIME_ALL && !isDragSeekBarFromUser) {
      updateProgressByHandler()
      updateSeekBarByHandler()
    }
  }

  fun showLyricOffsetView() {
    //todo
    if (view_pager.currentItem != 2) {
      view_pager.setCurrentItem(2, true)
    }
    lyricFragment.showLyricOffsetView()
  }

  private inner class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      //更新下一首歌曲
      next_song.text = getString(R.string.next_song, getNextSong().title)
    }
  }

  companion object {
    private const val TAG = "PlayerActivity"
    private const val UPDATE_BG = 1
    private const val UPDATE_TIME_ONLY = 2
    private const val UPDATE_TIME_ALL = 3

    const val BOTTOM_SHOW_NEXT = 0
    const val BOTTOM_SHOW_VOLUME = 1
    const val BOTTOM_SHOW_BOTH = 2
    const val BOTTOM_SHOW_NONE = 3

    const val BACKGROUND_THEME = 0
    const val BACKGROUND_ADAPTIVE_COLOR = 1
    const val BACKGROUND_CUSTOM_IMAGE = 2

    private const val FRAGMENT_COUNT = 2
    private const val DELAY_SHOW_NEXT_SONG = 3000

    const val ACTION_UPDATE_NEXT = "remix.myplayer.update.next_song"
  }
}