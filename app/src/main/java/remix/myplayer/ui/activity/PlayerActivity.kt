package remix.myplayer.ui.activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityPlayerBinding
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.getDuration
import remix.myplayer.helper.MusicServiceRemote.getNextSong
import remix.myplayer.helper.MusicServiceRemote.getOperation
import remix.myplayer.helper.MusicServiceRemote.getPlayModel
import remix.myplayer.helper.MusicServiceRemote.getProgress
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.helper.MusicServiceRemote.setPlayModel
import remix.myplayer.lyrics.provider.UriProvider
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.misc.menu.AudioPopupListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.theme.DrawableGradient
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.adapter.PagerAdapter
import remix.myplayer.ui.blur.StackBlurManager
import remix.myplayer.ui.dialog.PlayQueueDialog
import remix.myplayer.ui.dialog.PlayQueueDialog.Companion.newInstance
import remix.myplayer.ui.fragment.LyricsFragment
import remix.myplayer.ui.fragment.RecordFragment
import remix.myplayer.ui.fragment.player.CoverFragment
import remix.myplayer.ui.fragment.player.RoundCoverFragment
import remix.myplayer.ui.widget.LyricsView
import remix.myplayer.util.*
import remix.myplayer.util.SPUtil.SETTING_KEY
import timber.log.Timber
import java.io.File
import kotlin.math.abs

/**
 * Created by Remix on 2015/12/1.
 */
/**
 * 播放界面
 */
class PlayerActivity : BaseMusicActivity() {
  private lateinit var binding: ActivityPlayerBinding

  private var valueAnimator: ValueAnimator? = null

  //上次选中的Fragment
  private var prevPosition = 0

  //第一次启动的标志变量
  private var firstStart = true

  //是否正在拖动进度条
  var isDragSeekBarFromUser = false

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
  lateinit var lyricsFragment: LyricsFragment
    private set
  private lateinit var coverFragment: RoundCoverFragment

  private var distanceX = 0f
  private var distanceY = 0f
  private var lastY = 0f
  private var lastX = 0f

  private val thresholdX by lazy {
    DensityUtil.dip2px(App.context, 40f)
  }
  private val thresholdY by lazy {
    if (this.isPortraitOrientation()) {
      DensityUtil.dip2px(App.context, 100f)
    } else {
      DensityUtil.dip2px(App.context, 40f)
    }
  }

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
    makeAndStartAnimation(binding.layoutPlayerVolume.nextSong, true)
    makeAndStartAnimation(binding.layoutPlayerVolume.volumeContainer, false)
  }
  private val receiver: Receiver = Receiver()

  private val background by lazy {
    SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAYER_BACKGROUND, BACKGROUND_ADAPTIVE_COLOR)
  }

  val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
      MusicServiceRemote.service?.updateLyrics(UriProvider(it))
      lyricsFragment.updateLyrics()
    }
  }

  override fun setUpTheme() {
//    if (ThemeStore.isLightTheme()) {
//      super.setUpTheme();
//    } else {
//      setTheme(R.style.AudioHolderStyle_Night);
//    }
    val superThemeRes = ThemeStore.themeRes
    val themeRes: Int = when (superThemeRes) {
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
      Theme.setLightNavigationBarAuto(this, ColorUtil.isColorLight(navigationColor))
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
          Single
              .fromCallable {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val blurBitmap = StackBlurManager(bitmap).processNatively(10)

                blurBitmap
              }
              .compose(RxUtil.applySingleScheduler())
              .onErrorReturn {
                return@onErrorReturn BitmapFactory.decodeResource(resources, R.drawable.album_empty_bg_day)
              }
              .subscribe({
                binding.playerContainer.background = BitmapDrawable(resources, it)
                updateSwatch(it)
              }, {

              })
        }
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityPlayerBinding.inflate(layoutInflater)
    setContentView(binding.root)

    song = getCurrentSong()
    if (song == Song.EMPTY_SONG && intent.hasExtra(EXTRA_SONG)) {
      song = try {
        intent.getSerializableExtra(EXTRA_SONG) as Song
      } catch (e: Exception) {
        Timber.w("failed to load song: $e")
        Song.EMPTY_SONG
      }
    }

    setUpBottom()
    setUpTop()
    setUpFragments()
    setUpIndicator()
    setUpSeekBar()
    setUpViewColor()
    Util.registerLocalReceiver(receiver, IntentFilter(ACTION_UPDATE_NEXT))

    arrayOf(
        binding.layoutPlayerControl.playbarNext,
        binding.layoutPlayerControl.playbarPrev,
        binding.layoutPlayerControl.playbarPlayContainer
    ).forEach {
      it.setOnClickListener(onCtrlClick)
    }
    arrayOf(
        binding.layoutPlayerControl.playbarModel,
        binding.layoutPlayerControl.playbarPlayinglist,
        binding.topActionbar.topHide,
        binding.topActionbar.topMore
    ).forEach {
      it.setOnClickListener(onOtherClick)
    }
    arrayOf(
        binding.layoutPlayerVolume.volumeDown,
        binding.layoutPlayerVolume.volumeUp,
        binding.layoutPlayerVolume.nextSong
    ).forEach {
      it.setOnClickListener(onVolumeClick)
    }
  }

  public override fun onResume() {
    super.onResume()
//    if (this.isPortraitOrientation()) {
//      binding.viewPager.currentItem = 0
//    }
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
    coverFragment.clearAnim()
    overridePendingTransition(0, R.anim.audio_out)
  }

  /**
   * 上一首 下一首 播放、暂停
   */
  private val onCtrlClick = View.OnClickListener { v ->
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
  private val onOtherClick = View.OnClickListener { v ->
    when (v.id) {
      R.id.playbar_model -> {
        var currentModel = getPlayModel()
        currentModel = if (currentModel == Constants.MODE_REPEAT) Constants.MODE_LOOP else ++currentModel
        setPlayModel(currentModel)
        binding.layoutPlayerControl.playbarModel.setImageDrawable(Theme.tintDrawable(when (currentModel) {
          Constants.MODE_LOOP -> R.drawable.play_btn_loop
          Constants.MODE_SHUFFLE -> R.drawable.play_btn_shuffle
          else -> R.drawable.play_btn_loop_one
        }, ThemeStore.playerBtnColor))
        val msg = if (currentModel == Constants.MODE_LOOP) getString(R.string.model_normal) else if (currentModel == Constants.MODE_SHUFFLE) getString(R.string.model_random) else getString(R.string.model_repeat)
        //刷新下一首
        if (currentModel != Constants.MODE_SHUFFLE) {
          binding.layoutPlayerVolume.nextSong.text = getString(R.string.next_song, getNextSong().title)
        }
        ToastUtil.show(this, msg)
      }
      R.id.playbar_playinglist -> newInstance()
          .show(supportFragmentManager, PlayQueueDialog::class.java.simpleName)
      R.id.top_hide -> onBackPressed()
      R.id.top_more -> {
        @SuppressLint("RestrictedApi") val popupMenu = PopupMenu(this, v, Gravity.TOP)
        popupMenu.menuInflater.inflate(R.menu.menu_audio_item, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(AudioPopupListener(this, song))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          popupMenu.menu.removeItem(R.id.menu_speed)
        }
        popupMenu.show()
      }
    }
  }

  private val onVolumeClick = View.OnClickListener { v ->
    when (v.id) {
      R.id.volume_down, R.id.volume_up -> {
        launch {
          audioManager.adjustStreamVolume(
            STREAM_MUSIC,
            when (v.id) {
              R.id.volume_down -> AudioManager.ADJUST_LOWER
              R.id.volume_up -> AudioManager.ADJUST_RAISE
              else -> AudioManager.ADJUST_SAME
            },
            AudioManager.FLAG_PLAY_SOUND
          )
          updateVolumeSeekbar()
        }
      }
      R.id.next_song -> if (bottomConfig == BOTTOM_SHOW_BOTH) {
        makeAndStartAnimation(binding.layoutPlayerVolume.nextSong, false)
        makeAndStartAnimation(binding.layoutPlayerVolume.volumeContainer, true)
        handler.removeCallbacks(volumeRunnable)
        handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG)
      }
    }
  }

  private fun makeAndStartAnimation(view: View, show: Boolean) {
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
    view.startAnimation(alphaAnimation)
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
        .color(ThemeStore.accentColor)
        .make()
    normalIndicator = GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(ThemeStore.accentColor)
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
    duration = song.duration.toInt()
    val temp = getProgress()
    currentTime = if (temp in 1 until duration) temp else 0
    if (duration > 0 && duration - currentTime > 0) {
      binding.textHasplay.text = Util.getTime(currentTime.toLong())
      binding.textRemain.text = Util.getTime((duration - currentTime).toLong())
    }

    //初始化seekbar
    if (duration > 0 && duration < Int.MAX_VALUE) {
      binding.seekbar.max = duration
    } else {
      binding.seekbar.max = 1000
    }
    if (currentTime in 1 until duration) {
      binding.seekbar.progress = currentTime
    } else {
      binding.seekbar.progress = 0
    }
    binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
          updateProgressText(progress)
          lyricsFragment.updateProgress()
        }
        handler.sendEmptyMessage(UPDATE_TIME_ONLY)
        currentTime = progress
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

    launch {
      val volumeSeekbar = binding.layoutPlayerVolume.volumeSeekbar
      val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        audioManager.getStreamMinVolume(STREAM_MUSIC)
      } else 0
      val max = audioManager.getStreamMaxVolume(STREAM_MUSIC)
      val current = audioManager.getStreamVolume(STREAM_MUSIC)

      runOnUiThread {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          volumeSeekbar.min = min
        }
        volumeSeekbar.max = max
        volumeSeekbar.progress = current

        volumeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
          override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (bottomConfig == BOTTOM_SHOW_BOTH) {
              handler.removeCallbacks(volumeRunnable)
              handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG)
            }
            if (fromUser) {
              audioManager.setStreamVolume(
                STREAM_MUSIC,
                progress,
                AudioManager.FLAG_PLAY_SOUND
              )
            }
          }

          override fun onStartTrackingTouch(seekBar: SeekBar) {}
          override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
      }
    }
    if (bottomConfig == BOTTOM_SHOW_BOTH) {
      handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG)
    }
  }

  /**
   * 更新顶部歌曲信息
   */
  private fun updateTopStatus(song: Song?) {
    if (song == null) {
      return
    }
    val title = song.title
    val artist = song.artist
    val album = song.album
    if (title == "") {
      binding.topActionbar.topTitle.text = getString(R.string.unknown_song)
    } else {
      binding.topActionbar.topTitle.text = title
    }
    when {
      artist == "" -> {
        binding.topActionbar.topDetail.text = song.album
      }
      album == "" -> {
        binding.topActionbar.topDetail.text = song.artist
      }
      else -> {
        binding.topActionbar.topDetail.text = String.format("%s-%s", song.artist, song.album)
      }
    }
  }

  /**
   * 更新播放、暂停按钮
   */
  private fun updatePlayButton(isPlay: Boolean) {
    isPlaying = isPlay
    binding.layoutPlayerControl.playbarPlayPause.updateState(isPlay, true)
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
      if (fragment is LyricsFragment ||
          fragment is CoverFragment<*> ||
          fragment is RecordFragment) {
        fragmentManager.beginTransaction().remove(fragment).commitNow()
      }
    }
    coverFragment = RoundCoverFragment()
    setUpCoverFragment()
    lyricsFragment = LyricsFragment()
    setUpLyricsFragment()

    if (this.isPortraitOrientation()) {

      //Viewpager
      val adapter = PagerAdapter(supportFragmentManager)
      //      adapter.addFragment(mRecordFragment);
      adapter.addFragment(coverFragment)
      adapter.addFragment(lyricsFragment)
      binding.viewPager.adapter = adapter
      binding.viewPager.offscreenPageLimit = adapter.count - 1
      binding.viewPager.currentItem = 0

      binding.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
          indicators[prevPosition].setImageDrawable(normalIndicator)
          indicators[position].setImageDrawable(highLightIndicator)
          prevPosition = position
          //歌词界面常亮
          if (position == 1 && SPUtil
                  .getValue(this@PlayerActivity, SETTING_KEY.NAME, SETTING_KEY.SCREEN_ALWAYS_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
        }

        override fun onPageScrollStateChanged(state: Int) {}
      })
    } else {
      fragmentManager
          .beginTransaction()
          .replace(R.id.container_cover, coverFragment)
          .replace(R.id.container_lyric, lyricsFragment)
          .commit()
    }

    //歌词界面常亮
    if (SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SCREEN_ALWAYS_ON,
            false)) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        if (touchOnCoverFragment(event)) {
          lastX = event.x
          lastY = event.y
        }
      }
      MotionEvent.ACTION_MOVE -> {
        if (lastX > 0 && lastY > 0 && touchOnCoverFragment(event)) {
          distanceX += abs(event.x - lastX)
          distanceY += (event.y - lastY)
          if (distanceY > thresholdY && distanceX < thresholdX) {
            onBackPressed()
          }

          lastX = event.x
          lastY = event.y
        }
      }
      else -> {
        lastX = 0f
        lastY = 0f
        distanceY = 0f
        distanceX = 0f
      }
    }

    return super.dispatchTouchEvent(event)
  }

  private fun touchOnCoverFragment(event: MotionEvent): Boolean {
    val rect = Rect()
    return (this.isPortraitOrientation() && binding.viewPager.currentItem == 0) ||
        (binding.containerCover?.getLocalVisibleRect(rect) == true && rect.contains(event.x.toInt(), event.y.toInt()))
  }


  private fun setUpLyricsFragment() {
    lyricsFragment.onSeekToListener = LyricsView.OnSeekToListener { progress ->
      if (progress in 0..getDuration()) {
        MusicServiceRemote.setProgress(progress)
        currentTime = progress
        handler.sendEmptyMessage(UPDATE_TIME_ALL)
      }
    }
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
    lyricsFragment.updateLyrics() // TODO
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
      lyricsFragment.updateLyrics() // TODO: is it needed?
      //更新进度条
      val temp = getProgress()
      currentTime = if (temp in 1 until duration) temp else 0
      duration = song.duration.toInt()
      binding.seekbar.max = duration
      //更新下一首歌曲
      binding.layoutPlayerVolume.nextSong.text = getString(R.string.next_song, getNextSong().title)
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
      while (isForeground) {
        try {
          //音量
          if (binding.layoutPlayerVolume.volumeSeekbar.visibility == View.VISIBLE) {
            updateVolumeSeekbar()
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
        } catch (ignore: Exception) {
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
    bottomConfig = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN, BOTTOM_SHOW_BOTH)
    if (!this.isPortraitOrientation()) { //横屏不显示底部
      bottomConfig = BOTTOM_SHOW_NONE
    }
    when (bottomConfig) {
      BOTTOM_SHOW_NEXT -> { //仅显示下一首
        binding.layoutPlayerVolume.volumeContainer.visibility = View.GONE
        binding.layoutPlayerVolume.nextSong.visibility = View.VISIBLE
      }
      BOTTOM_SHOW_VOLUME -> { //仅显示音量控制
        binding.layoutPlayerVolume.volumeContainer.visibility = View.VISIBLE
        binding.layoutPlayerVolume.nextSong.visibility = View.GONE
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
    val accentColor = ThemeStore.accentColor
    val tintColor = ThemeStore.playerBtnColor
    updateSeekBarColor(accentColor)
    //        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(context,10),DensityUtil.dip2px(context,10)));
//        Drawable seekbarBackground = mProgressSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

    //修改控制按钮颜色
    Theme.tintDrawable(binding.layoutPlayerControl.playbarNext, R.drawable.play_btn_next, accentColor)
    Theme.tintDrawable(binding.layoutPlayerControl.playbarPrev, R.drawable.play_btn_pre, accentColor)
    binding.layoutPlayerControl.playbarPlayPause.setBackgroundColor(accentColor)

    //歌曲名颜色
    binding.topActionbar.topTitle.setTextColor(ThemeStore.playerTitleColor)

    //修改顶部按钮颜色
    Theme.tintDrawable(binding.topActionbar.topHide, R.drawable.icon_player_back, tintColor)
    Theme.tintDrawable(binding.topActionbar.topMore, R.drawable.icon_player_more, tintColor)
    //播放模式与播放队列
    val playMode = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL,
        Constants.MODE_LOOP)
    Theme.tintDrawable(binding.layoutPlayerControl.playbarModel, if (playMode == Constants.MODE_LOOP) R.drawable.play_btn_loop else if (playMode == Constants.MODE_SHUFFLE) R.drawable.play_btn_shuffle else R.drawable.play_btn_loop_one, tintColor)
    Theme.tintDrawable(binding.layoutPlayerControl.playbarPlayinglist, R.drawable.play_btn_normal_list, tintColor)

    //音量控制
    binding.layoutPlayerVolume.volumeDown.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
    binding.layoutPlayerVolume.volumeUp.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
    //        Theme.tintDrawable(mVolumeDown,R.drawable.ic_volume_down_black_24dp,tintColor);
//        Theme.tintDrawable(mVolumeUp,R.drawable.ic_volume_up_black_24dp,tintColor);
    //下一首背景
    binding.layoutPlayerVolume.nextSong.background = GradientDrawableMaker()
        .color(ThemeStore.playerNextSongBgColor)
        .corner(DensityUtil.dip2px(2f).toFloat())
        .width(DensityUtil.dip2px(288f))
        .height(DensityUtil.dip2px(38f))
        .make()
    binding.layoutPlayerVolume.nextSong.setTextColor(ThemeStore.playerNextSongTextColor)
  }

  private fun updateSeekBarColor(color: Int) {
    setProgressDrawable(binding.seekbar, color)
    setProgressDrawable(binding.layoutPlayerVolume.volumeSeekbar, color)

    //修改thumb
    val inset = DensityUtil.dip2px(this, 6f)
    val width = DensityUtil.dip2px(this, 2f)
    val height = DensityUtil.dip2px(this, 6f)
    binding.seekbar.thumb = InsetDrawable(
        GradientDrawableMaker()
            .width(width)
            .height(height)
            .color(color)
            .make(),
        inset, inset, inset, inset)
    binding.layoutPlayerVolume.volumeSeekbar.thumb = InsetDrawable(GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(color)
        .make(),
        inset, inset, inset, inset)
  }

  private fun setProgressDrawable(seekBar: SeekBar, color: Int) {
    val progressDrawable = seekBar.progressDrawable as LayerDrawable
    //修改progress颜色
    (progressDrawable.getDrawable(0) as GradientDrawable).setColor(ThemeStore.playerProgressColor)
    progressDrawable.getDrawable(1).setColorFilter(color, PorterDuff.Mode.SRC_IN)
    seekBar.progressDrawable = progressDrawable
  }

  private fun updateBackground(uri: Uri) {
//    object : ImageUriRequest<Bitmap>(RequestConfig.Builder(100, 100).build()) {
//      override fun onError(throwable: Throwable?) {
//        Timber.v("updateBackground failed: $throwable")
//        updateSwatch(null)
//      }
//
//      override fun onSuccess(result: Bitmap?) {
//        updateSwatch(result)
//      }
//
//      override fun load(): Disposable {
//        return getThumbBitmapObservable(uri)
//            .compose(RxUtil.applyScheduler())
//            .subscribe({
//              onSuccess(it)
//            }, {
//              onError(it)
//            })
//      }
//    }.load()

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
    binding.layoutPlayerControl.playbarNext.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
    binding.layoutPlayerControl.playbarPrev.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
    binding.layoutPlayerControl.playbarPlayPause.setBackgroundColor(swatch.rgb)

    binding.layoutPlayerVolume.volumeDown.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
    binding.layoutPlayerVolume.volumeUp.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

    binding.layoutPlayerControl.playbarModel.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
    binding.layoutPlayerControl.playbarPlayinglist.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

    normalIndicator.setColor(ColorUtil.adjustAlpha(swatch.rgb, 0.3f))
    highLightIndicator.setColor(swatch.rgb)

    updateSeekBarColor(swatch.rgb)
    binding.layoutPlayerVolume.nextSong.setBackgroundColor(ColorUtil.adjustAlpha(swatch.rgb, 0.1f))

    binding.topActionbar.topTitle.setTextColor(swatch.titleTextColor)
    binding.topActionbar.topDetail.setTextColor(swatch.bodyTextColor)

    binding.topActionbar.topMore.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
    binding.topActionbar.topHide.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
  }

  private fun startBGColorAnimation(swatch: Swatch) {
    valueAnimator?.cancel()

    val surfaceColor = Theme.resolveColor(this, R.attr.colorSurface, if (ThemeStore.isLightTheme) Color.WHITE else Color.BLACK)
    valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), surfaceColor, swatch.rgb)

    valueAnimator?.addUpdateListener { animation ->
      val drawable = DrawableGradient(GradientDrawable.Orientation.TOP_BOTTOM,
          intArrayOf(animation.animatedValue as Int,
              surfaceColor
          ), 0)
      binding.playerContainer.background = drawable
    }
    valueAnimator?.setDuration(1000)?.start()
  }

  override fun onDestroy() {
    super.onDestroy()
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    handler.remove()
    Util.unregisterLocalReceiver(receiver)
  }

  private fun updateProgressText(current: Int) {
    if (current > 0 && duration - current > 0) {
      binding.textHasplay.text = Util.getTime(current.toLong())
      binding.textRemain.text = Util.getTime((duration - current).toLong())
    }
  }

  private fun updateProgressByHandler() {
    updateProgressText(currentTime)
  }

  private fun updateSeekBarByHandler() {
    binding.seekbar.progress = currentTime
  }

  private fun updateVolumeSeekbar() {
    val volume = audioManager.getStreamVolume(STREAM_MUSIC)
    runOnUiThread {
      binding.layoutPlayerVolume.volumeSeekbar.progress = volume
    }
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
    if (binding.viewPager.currentItem != 2) {
      binding.viewPager.setCurrentItem(2, true)
    }
    lyricsFragment.showOffsetPanel()
  }

  private inner class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      //更新下一首歌曲
      binding.layoutPlayerVolume.nextSong.text = getString(R.string.next_song, getNextSong().title)
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
    private const val DELAY_SHOW_NEXT_SONG: Long = 3000

    const val ACTION_UPDATE_NEXT = "remix.myplayer.update.next_song"
  }
}