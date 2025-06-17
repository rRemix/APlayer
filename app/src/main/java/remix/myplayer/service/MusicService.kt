package remix.myplayer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import androidx.annotation.WorkerThread
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.*
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.appwidgets.BaseAppwidget
import remix.myplayer.appwidgets.big.AppWidgetBig
import remix.myplayer.appwidgets.medium.AppWidgetMedium
import remix.myplayer.appwidgets.medium.AppWidgetMediumTransparent
import remix.myplayer.appwidgets.small.AppWidgetSmall
import remix.myplayer.appwidgets.small.AppWidgetSmallTransparent
import remix.myplayer.bean.mp3.Song
import remix.myplayer.bean.mp3.Song.Companion.EMPTY_SONG
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.helper.*
import remix.myplayer.lyrics.LyricsManager
import remix.myplayer.misc.getPendingIntentFlag
import remix.myplayer.misc.log.LogObserver
import remix.myplayer.misc.observer.MediaStoreObserver
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.misc.receiver.HeadsetPlugReceiver.Companion.NEVER
import remix.myplayer.misc.receiver.HeadsetPlugReceiver.Companion.OPEN_SOFTWARE
import remix.myplayer.misc.receiver.MediaButtonReceiver
import remix.myplayer.misc.tryLaunch
import remix.myplayer.service.notification.Notify
import remix.myplayer.service.notification.NotifyImpl
import remix.myplayer.service.notification.NotifyImpl24
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.LockScreenActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PERMISSION
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import remix.myplayer.util.*
import remix.myplayer.util.Constants.*
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.Util.isAppOnForeground
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放Service 歌曲的播放 控制 回调相关activity的界面更新 通知栏的控制
 */
@SuppressLint("CheckResult")
class MusicService : BaseService(), Playback, MusicEventCallback,
    MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
    SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope by MainScope() {
  // 播放队列
  private val playQueue = PlayQueue(this)

  // 当前播放的歌曲
  val currentSong: Song
    get() = playQueue.song

  // 下一首歌曲
  val nextSong: Song
    get() = playQueue.nextSong

  /**
   * 是否第一次准备完成
   */
  private var firstPrepared = true

  /**
   * 是否正在设置mediapplayer的datasource
   */
  private var prepared = false

  /**
   * 数据加载
   */
  @Volatile
  private var load = 0
  private val LOADING = 1
  private val LOAD_SUCCESS = 2

  /**
   * 设置播放模式并更新下一首歌曲
   */
  var playModel: Int = MODE_LOOP
    set(newPlayModel) {
      Timber.v("修改播放模式: $newPlayModel")
      val fromShuffleToNone = field == MODE_SHUFFLE
      field = newPlayModel
      desktopWidgetTask?.run()
      SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL, newPlayModel)

      // 从随机播放切换到非随机播放 需要根据当前播放的歌曲 重新确定position
      if (fromShuffleToNone) {
        playQueue.rePosition()
      }
      playQueue.makeList()
      playQueue.updateNextSong()
      updateQueueItem()
    }

  /**
   * 当前播放的歌曲是否收藏
   */
  var isLove: Boolean = false

  /**
   * 播放完当前歌曲后是否停止app
   */
  private var pendingClose: Boolean = false

  /**
   * MediaPlayer 负责歌曲的播放等
   */
  var mediaPlayer: MediaPlayer = MediaPlayer()

  /**
   * 桌面部件
   */
  private val appWidgets: HashMap<String, BaseAppwidget> = HashMap()

  /**
   * AudioManager
   */
  private val audioManager: AudioManager by lazy {
    getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  /**
   * 播放控制的Receiver
   */
  private val controlReceiver: ControlReceiver by lazy {
    ControlReceiver()
  }

  /**
   * 事件
   */
  private val musicEventReceiver: MusicEventReceiver by lazy {
    MusicEventReceiver()
  }

  /**
   * 监测耳机拔出的Receiver
   */
  private val headSetReceiver: HeadsetPlugReceiver by lazy {
    HeadsetPlugReceiver()
  }

  /**
   * 接收桌面部件
   */
  private val widgetReceiver: WidgetReceiver by lazy {
    WidgetReceiver()
  }

  /**
   * 监听AudioFocus的改变
   */
  private val audioFocusListener by lazy {
    AudioFocusChangeListener()
  }

  /**
   * MediaSession
   */
  lateinit var mediaSession: MediaSessionCompat
    private set

  /**
   * 当前是否获得AudioFocus
   */
  private var audioFocus = false

  private val audioAttributes = AudioAttributesCompat.Builder().run {
    setUsage(AudioAttributesCompat.USAGE_MEDIA)
    setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
    build()
  }

  private val focusRequest =
      AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).run {
        setAudioAttributes(audioAttributes)
        setOnAudioFocusChangeListener(audioFocusListener)
        build()
      }

  /**
   * 更新相关Activity的Handler
   */
  private val uiHandler = PlaybackHandler(this)

  /**
   * 电源锁
   */
  private val wakeLock: PowerManager.WakeLock by lazy {
    (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.simpleName)
  }

  /**
   * 通知栏
   */
  private lateinit var notify: Notify

  /**
   * 当前控制命令
   */
  private var control: Int = 0

  /**
   * service是否停止运行
   */
  var stop = true

  /**
   * 监听锁屏
   */
  private val screenReceiver: ScreenReceiver by lazy {
    ScreenReceiver()
  }
  private var screenOn = true
    set(value) {
      field = value
      LyricsManager.isScreenOn = value
    }

  /**
   * 音量控制
   */
  private val volumeController: VolumeController by lazy {
    VolumeController(this)
  }

  /**
   * 退出时播放的进度
   */
  private var lastProgress: Int = 0

  /**
   * 是否开启断点播放
   */
  private var playAtBreakPoint: Boolean = false
  private var progressTask: ProgressTask? = null

  /**
   * 操作类型
   */
  var operation = -1

  /**
   * Binder
   */
  private val musicBinder = MusicBinder()

  /**
   * 数据库
   */
  private val repository = DatabaseRepository.getInstance()

  /**
   * 监听Mediastore变化
   */
  private val mediaStoreObserver: MediaStoreObserver by lazy {
    MediaStoreObserver()
  }
  private lateinit var service: MusicService

  private var hasPermission = false

  private var alreadyUnInit: Boolean = false
  var speed = 1.0f
    private set


  /**
   * 当前是否正在播放
   */
  var isPlaying: Boolean = false
    private set(value) {
      field = value
      uiHandler.sendEmptyMessage(UPDATE_PLAY_STATE)
    }

  /**
   * 获得当前播放进度
   */
  val progress: Int
    get() {
      try {
        if (prepared) {
          return mediaPlayer.currentPosition
        }
      } catch (e: IllegalStateException) {
        Timber.v("getProgress() %s", e.toString())
      }

      return 0
    }

  val duration: Int
    get() = if (prepared) {
      mediaPlayer.duration
    } else 0

  /**
   * 保存当前进度&更新桌面部件
   */
  private var timer: Timer = Timer()
  private var desktopWidgetTask: WidgetTask? = null

  /**
   * 锁屏
   */
  private var lockScreen = CLOSE_LOCKSCREEN

  private var crossFade = true

  override fun onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)
    Timber.tag(TAG_LIFECYCLE).v("onTaskRemoved")
//    unInit()
//    stopSelf()
//    System.exit(0)
  }

  override fun onDestroy() {
    Timber.tag(TAG_LIFECYCLE).v("onDestroy")
    super.onDestroy()
    stop = true
    unInit()
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(LanguageHelper.setLocal(base))
  }

  override fun onCreate() {
    super.onCreate()
    Timber.tag(TAG_LIFECYCLE).v("onCreate")
    service = this
    setUp()
  }

  override fun onBind(intent: Intent): IBinder? {
    return musicBinder
  }

  inner class MusicBinder : Binder() {
    val service: MusicService
      get() = this@MusicService
  }

  @SuppressLint("CheckResult")
  override fun onStartCommand(commandIntent: Intent?, flags: Int, startId: Int): Int {
    val control = commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
    val action = commandIntent?.action

    Timber.v("onStartCommand, control: $control action: $action flags: $flags startId: $startId")
    stop = false

    tryLaunch {
      hasPermission = PermissionUtil.hasNecessaryPermission()
      withContext(Dispatchers.IO) {
        load()
      }
      delay(200)
      handleStartCommandIntent(commandIntent, action)
    }
    return START_NOT_STICKY
  }

  override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
//    Timber.v("onSharedPreferenceChanged, key: $key")
    when (key) {
      //通知栏背景色
      SETTING_KEY.NOTIFY_SYSTEM_COLOR,
        //通知栏样式
      SETTING_KEY.NOTIFY_STYLE_CLASSIC -> {
        val classic = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)
        notify = if (classic) {
          NotifyImpl(this@MusicService)
        } else {
          NotifyImpl24(this@MusicService)
        }
        if (Notify.isNotifyShowing) {
          // 先取消再重新显示 让通知栏彻底刷新一次
          notify.cancelPlayingNotify()
          updateNotification()
        }
      }
      //锁屏
      SETTING_KEY.LOCKSCREEN -> {
        lockScreen = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, APLAYER_LOCKSCREEN)
        when (lockScreen) {
          CLOSE_LOCKSCREEN -> clearMediaSession()
          SYSTEM_LOCKSCREEN, APLAYER_LOCKSCREEN -> updateMediaSession(Command.NEXT)
        }
      }
      //断点播放
      SETTING_KEY.PLAY_AT_BREAKPOINT -> {
        playAtBreakPoint = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_AT_BREAKPOINT, false)
        if (!playAtBreakPoint) {
          stopSaveProgress()
        } else {
          startSaveProgress()
        }
      }
      //倍速播放
      SETTING_KEY.SPEED -> {
        speed = java.lang.Float.parseFloat(SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SPEED, "1.0"))
        setSpeed(speed)
      }
      // 淡入淡出
      SETTING_KEY.CROSS_FADE -> {
        crossFade = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CROSS_FADE, true)
      }
    }
  }

  private fun setUp() {
    //配置变化
    getSharedPreferences(SETTING_KEY.NAME, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this)

    //电源锁
    wakeLock.setReferenceCounted(false)
    //通知栏
    notify = if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) and
        !SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)) {
      NotifyImpl24(this)
    } else {
      NotifyImpl(this)
    }

    //桌面部件
    appWidgets[APPWIDGET_BIG] = AppWidgetBig.getInstance()
    appWidgets[APPWIDGET_MEDIUM] = AppWidgetMedium.getInstance()
    appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] = AppWidgetMediumTransparent.getInstance()
    appWidgets[APPWIDGET_SMALL] = AppWidgetSmall.getInstance()
    appWidgets[APPWIDGET_SMALL_TRANSPARENT] = AppWidgetSmallTransparent.getInstance()

    //初始化Receiver
    val eventFilter = IntentFilter()
    eventFilter.addAction(MEDIA_STORE_CHANGE)
    eventFilter.addAction(PERMISSION_CHANGE)
    eventFilter.addAction(PLAYLIST_CHANGE)
    eventFilter.addAction(TAG_CHANGE)
    registerLocalReceiver(musicEventReceiver, eventFilter)

    registerLocalReceiver(controlReceiver, IntentFilter(ACTION_CMD))

    val noisyFilter = IntentFilter()
    noisyFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG)
    registerReceiver(headSetReceiver, noisyFilter)

    registerLocalReceiver(widgetReceiver, IntentFilter(ACTION_WIDGET_UPDATE))

    val screenFilter = IntentFilter()
    screenFilter.addAction(Intent.ACTION_SCREEN_ON)
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
    App.context.registerReceiver(screenReceiver, screenFilter)

    //监听数据库变化
    contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)

    //定时关闭
    SleepTimer.addCallback(object : SleepTimer.Callback {
      override fun onFinish() {
        if (SPUtil.getValue(
                this@MusicService,
                SETTING_KEY.NAME,
                SETTING_KEY.TIMER_EXIT_AFTER_FINISH,
                false
            )
        ) {
          pendingClose = true
        } else {
          sendBroadcast(
              Intent(ACTION_EXIT).setComponent(
                  ComponentName(
                      this@MusicService, ExitReceiver::class.java
                  )
              )
          )
        }
      }

      override fun revert() {
        pendingClose = false
      }
    })

    LyricsManager.isServiceAvailable = true

    setUpPlayer()
    setUpSession()
  }

  /**
   * 初始化mediasession
   */
  private fun setUpSession() {
    val mediaButtonReceiverComponentName = ComponentName(applicationContext,
        MediaButtonReceiver::class.java)

    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
    mediaButtonIntent.component = mediaButtonReceiverComponentName

    val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, getPendingIntentFlag())

    mediaSession = MediaSessionCompat(applicationContext, "APlayer", mediaButtonReceiverComponentName, pendingIntent)
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
      override fun onMediaButtonEvent(event: Intent): Boolean {
        return MediaButtonReceiver.handleMediaButtonIntent(this@MusicService, event)
      }

      override fun onSkipToNext() {
        Timber.v("onSkipToNext")
        playNext()
      }

      override fun onSkipToPrevious() {
        Timber.v("onSkipToPrevious")
        playPrevious()
      }

      override fun onPlay() {
        Timber.v("onPlay")
        play(true)
      }

      override fun onPause() {
        Timber.v("onPause")
        pause(false)
      }

      override fun onStop() {
        pause(false)
        notify.cancelPlayingNotify()
        stopSelf()
      }

      override fun onSeekTo(pos: Long) {
        setProgress(pos)
      }

      override fun onCustomAction(action: String?, extras: Bundle?) {
        Timber.v("onCustomAction, ac: $action extra: $extras")
        when (action) {
          ACTION_UNLOCK_DESKTOP_LYRIC -> LyricsManager.isDesktopLyricsLocked = false
          ACTION_TOGGLE_DESKTOP_LYRIC -> LyricsManager.setDesktopLyricsEnabled(!LyricsManager.isDesktopLyricsEnabled)
        }
      }
    })

    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
    mediaSession.setMediaButtonReceiver(pendingIntent)
    mediaSession.isActive = true
  }

  /**
   * 初始化Mediaplayer
   */
  private fun setUpPlayer() {
    mediaPlayer = MediaPlayer()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mediaPlayer.setAudioAttributes(audioAttributes.unwrap() as AudioAttributes)
    } else {
      @Suppress("DEPRECATION")
      mediaPlayer.setAudioStreamType(audioAttributes.legacyStreamType)
    }
    mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
    mediaPlayer.setOnCompletionListener(this)
    mediaPlayer.setOnPreparedListener(this)
    mediaPlayer.setOnBufferingUpdateListener(this)
    mediaPlayer.setOnErrorListener(this)

    EQHelper.init(this, mediaPlayer.audioSessionId)
    EQHelper.open(this, mediaPlayer.audioSessionId)
  }

  override fun onCompletion(mp: MediaPlayer?) {
    if (pendingClose) {
      Timber.v("发送Exit广播")
      sendBroadcast(Intent(ACTION_EXIT)
          .setComponent(ComponentName(this@MusicService, ExitReceiver::class.java)))
      return
    }
    if (playModel == MODE_REPEAT) {
      prepare(playQueue.song)
    } else {
      playNextOrPrev(true)
    }
    operation = Command.NEXT
    acquireWakeLock()
  }

  override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
    Timber.v("onBufferingUpdate, percent: $percent")
  }

  override fun onPrepared(mp: MediaPlayer?) {
    Timber.v("准备完成:%s", firstPrepared)

    prepared = true

    if (firstPrepared) {
      firstPrepared = false
      if (lastProgress > 0) {
        mediaPlayer.seekTo(lastProgress)
      }
      //自动播放
      if (SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, NEVER) != OPEN_SOFTWARE) {
        return
      }
    }

    Timber.v("开始播放")
    //记录播放历史
    updatePlayHistory()
    //开始播放
    play(false)
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    Timber.e("onError, what: $what extra: $extra")
    ToastUtil.show(service, R.string.mediaplayer_error, what, extra)
    prepared = false
    mediaPlayer.release()
    setUpPlayer()
    playNext()
    return true
  }

  /**
   * 更新播放历史
   */
  private fun updatePlayHistory() {
    if (playQueue.song.isLocal()) {
      repository.updateHistory(playQueue.song)
        .compose(applySingleScheduler())
        .subscribe(LogObserver())
    }
  }

  private fun unInit() {
    if (alreadyUnInit) {
      return
    }

    cancel()

    EQHelper.close(this, mediaPlayer.audioSessionId)
    if (isPlaying) {
      pause(false)
    }
    mediaPlayer.release()
    load = 0
    prepared = false

    timer.cancel()
    notify.cancelPlayingNotify()

    LyricsManager.isServiceAvailable = false

    uiHandler.removeCallbacksAndMessages(null)
    uiHandler.sendEmptyMessage(UPDATE_NOTIFICATION)

    AudioManagerCompat.abandonAudioFocusRequest(audioManager, focusRequest)

    mediaSession.isActive = false
    mediaSession.release()

    unregisterLocalReceiver(controlReceiver)
    unregisterLocalReceiver(musicEventReceiver)
    unregisterLocalReceiver(widgetReceiver)
    Util.unregisterReceiver(this, headSetReceiver)
    Util.unregisterReceiver(this, screenReceiver)

    getSharedPreferences(SETTING_KEY.NAME, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this)

    releaseWakeLock()

    contentResolver.unregisterContentObserver(mediaStoreObserver)

    ShakeDetector.getInstance().stopListen()

    alreadyUnInit = true
  }

  fun setAllSong(allSong: List<Song>?) {
//    allSong?.let {
//      this.allSong.clear()
//      this.allSong.addAll(it)
//    }
  }

  private fun updateQueueItem() {
    Timber.v("updateQueueItem")
    tryLaunch(block = {
      withContext(Dispatchers.IO) {
        val queue = ArrayList(playQueue.playingQueue)
            .map { song ->
              return@map MediaSessionCompat.QueueItem(MediaMetadataCompat.Builder()
                  .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
                  .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                  .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                  .build().description, song.id
              )
            }
        Timber.v("updateQueueItem, queue: ${queue.size}")
        mediaSession.setQueueTitle(playQueue.song.title)
        mediaSession.setQueue(queue)
      }
    }, catch = {
      ToastUtil.show(this, it.toString())
      Timber.w(it)
    })
  }

  /**
   * 设置播放队列
   */
  fun setPlayQueue(newQueue: List<Song>?) {
    Timber.v("setPlayQueue")
    if (newQueue.isNullOrEmpty()) {
      return
    }
    if (newQueue == playQueue.originalQueue) {
      return
    }

    playQueue.setPlayQueue(newQueue)
    updateQueueItem()
  }

  /**
   * 设置播放队列
   */
  fun setPlayQueue(newQueue: List<Song>?, intent: Intent) {
    Timber.v("setPlayQueue")
    //如果是随机播放，需要更新randomList
    val shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false)
    if (newQueue.isNullOrEmpty()) {
      return
    }

    //设置的播放队列相等
    val equals = newQueue == playQueue.originalQueue
    if (!equals) {
      playQueue.setPlayQueue(newQueue)
    }
    if (shuffle) {
      playModel = MODE_SHUFFLE
      playQueue.updateNextSong()
    }
    handleCommand(intent)

    if (equals) {
      return
    }
    updateQueueItem()
  }

  /**
   * 播放下一首
   */
  override fun playNext() {
    playNextOrPrev(true)
  }

  /**
   * 播放上一首
   */
  override fun playPrevious() {
    playNextOrPrev(false)
  }

  /**
   * 开始播放
   */
  override fun play(fadeIn: Boolean) {
    Timber.v("play: $fadeIn")
    if (!prepared) {
      ToastUtil.show(this, getString(R.string.buffering_wait))
      return
    }
    audioFocus = AudioManagerCompat.requestAudioFocus(
        audioManager,
        focusRequest
    ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    if (!audioFocus) {
      return
    }

    isPlaying = true

    //更新所有界面
    uiHandler.sendEmptyMessage(UPDATE_META_DATA)

    //播放
    mediaPlayer.start()

    //倍速播放
//    setSpeed(speed)

    //渐变
    if (fadeIn && crossFade) {
      volumeController.fadeIn()
    } else {
      volumeController.directTo(1f)
    }

    //保存当前播放和下一首播放的歌曲的id
    launch {
      withContext(Dispatchers.IO) {
        val song = playQueue.song
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_SONG, if (song.isLocal()) song.id.toString() else song.data)
      }
    }
  }


  /**
   * 根据当前播放状态暂停或者继续播放
   */
  override fun toggle() {
    Timber.v("toggle")
    if (mediaPlayer.isPlaying) {
      pause(false)
    } else {
      play(true)
    }
  }

  /**
   * 暂停
   */
  override fun pause(updateMediasessionOnly: Boolean) {
    Timber.v("pause: $updateMediasessionOnly")
    if (updateMediasessionOnly) {
      updateMediaSession(operation)
    } else {
      if (!isPlaying) { //如果当前已经暂停了 就不重复操作了 避免已经关闭了通知栏又再次显示
        return
      }
      isPlaying = false
      uiHandler.sendEmptyMessage(UPDATE_META_DATA)
      if (crossFade) {
        volumeController.fadeOut()
      } else {
        mediaPlayer.pause()
      }
    }
  }

  /**
   * 播放选中的歌曲 比如在全部歌曲或者专辑详情里面选中某一首歌曲
   *
   * @param position 播放位置
   */
  override fun playSelectSong(position: Int) {
    Timber.v("playSelectSong, $position")

    if (position == -1 || position >= playQueue.playingQueue.size) {
      ToastUtil.show(service, R.string.illegal_arg)
      return
    }

    playQueue.setPosition(position)

    if (playQueue.song.data.isEmpty()) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    prepare(playQueue.song)
    playQueue.updateNextSong()
  }

  override fun onMediaStoreChanged() {
//    launch {
//      val song = withContext(Dispatchers.IO) {
//        MediaStoreUtil.getSongById(playQueue.song.id)
//      }
//      playQueue.song = song
//    }
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission && has) {
      hasPermission = true
      loadSync()
    }
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {
//    // 改变的歌曲是当前播放的
//    if (oldSong.id == currentSong.id) {
//      currentSong = newSong
//      currentId = newSong.id
//    }
  }

  override fun onPlayListChanged(name: String) {
    if (name == PlayQueue.TABLE_NAME) {
      repository
          .getPlayQueueSongs()
          .compose(applySingleScheduler())
          .subscribe { songs ->
            if (songs.isEmpty() || songs == playQueue.originalQueue) {
              Timber.v("忽略onPlayListChanged")
              return@subscribe
            }
            Timber.v("新的播放队列: ${songs.size}")

            playQueue.setPlayQueue(songs)

            // 随机播放模式下重新设置下RandomQueue
            if (playModel == MODE_SHUFFLE) {
              Timber.v("播放队列改变后重新设置随机队列")
              playQueue.makeList()
            }

            // 如果下一首歌曲不在队列里面 重新设置下一首歌曲
            if (!playQueue.playingQueue.contains(playQueue.nextSong)) {
              Timber.v("播放队列改变后重新设置下一首歌曲")
              playQueue.updateNextSong()
            }
          }
    }
  }

  override fun onMetaChanged() {

  }

  override fun onPlayStateChange() {

  }

  override fun onServiceConnected(service: MusicService) {

  }

  override fun onServiceDisConnected() {

  }

  inner class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      //            final int skin = SPUtil.getValue(context,SETTING_KEY.NAME,SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
      //            SPUtil.putValue(context,SETTING_KEY.NAME, SETTING_KEY.APP_WIDGET_SKIN,skin == SKIN_WHITE_1F ? SKIN_TRANSPARENT : SKIN_WHITE_1F);

      val name = intent.getStringExtra(BaseAppwidget.EXTRA_WIDGET_NAME)
      val appIds = intent.getIntArrayExtra(BaseAppwidget.EXTRA_WIDGET_IDS)
      Timber.v("name: $name appIds: $appIds")
      when (name) {
        APPWIDGET_BIG -> if (appWidgets[APPWIDGET_BIG] != null) {
          appWidgets[APPWIDGET_BIG]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM -> if (appWidgets[APPWIDGET_MEDIUM] != null) {
          appWidgets[APPWIDGET_MEDIUM]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL -> if (appWidgets[APPWIDGET_SMALL] != null) {
          appWidgets[APPWIDGET_SMALL]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM_TRANSPARENT -> if (appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_MEDIUM_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL_TRANSPARENT -> if (appWidgets[APPWIDGET_SMALL_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_SMALL_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
      }
    }
  }

  inner class MusicEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      handleMusicEvent(intent)
    }
  }

  private fun handleStartCommandIntent(commandIntent: Intent?, action: String?) {
    Timber.v("handleStartCommandIntent")
    if (action == null) {
      return
    }
    firstPrepared = false
    when (action) {
      ACTION_APPWIDGET_OPERATE -> {
        val appwidgetIntent = Intent(ACTION_CMD)
        val control = commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
        if (control == UPDATE_APPWIDGET) {
          updateAppwidget()
        } else {
          appwidgetIntent.putExtra(EXTRA_CONTROL, control)
          handleCommand(appwidgetIntent)
        }
      }
      ACTION_SHORTCUT_SHUFFLE -> {
        if (playModel != MODE_SHUFFLE) {
          playModel = MODE_SHUFFLE
        }
        val shuffleIntent = Intent(ACTION_CMD)
        shuffleIntent.putExtra(EXTRA_CONTROL, Command.NEXT)
        handleCommand(shuffleIntent)
      }
      ACTION_SHORTCUT_MYLOVE -> {
        tryLaunch {
          val songs = withContext(Dispatchers.IO) {
            val myLoveIds = repository.getMyLoveList().blockingGet()
            MediaStoreUtil.getSongsByIds(myLoveIds)
          }

          if (songs == null || songs.isEmpty()) {
            ToastUtil.show(service, R.string.list_is_empty)
            return@tryLaunch
          }

          val myloveIntent = Intent(ACTION_CMD)
          myloveIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
          myloveIntent.putExtra(EXTRA_POSITION, 0)
          setPlayQueue(songs, myloveIntent)
        }

      }
      ACTION_SHORTCUT_LASTADDED -> {
        tryLaunch {
          val songs = withContext(Dispatchers.IO) {
            MediaStoreUtil.getLastAddedSong()
          }
          if (songs.isEmpty()) {
            ToastUtil.show(service, R.string.list_is_empty)
            return@tryLaunch
          }
          val lastedIntent = Intent(ACTION_CMD)
          lastedIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
          lastedIntent.putExtra(EXTRA_POSITION, 0)
          setPlayQueue(songs, lastedIntent)
        }

      }
      else -> if (action.equals(ACTION_CMD, ignoreCase = true)) {
        handleCommand(commandIntent)
      }
    }
  }

  private fun handleMusicEvent(intent: Intent?) {
    if (intent == null) {
      return
    }
    when (intent.action) {
      MEDIA_STORE_CHANGE -> onMediaStoreChanged()
      PERMISSION_CHANGE -> onPermissionChanged(intent.getBooleanExtra(EXTRA_PERMISSION, false))
      PLAYLIST_CHANGE -> onPlayListChanged(intent.getStringExtra(EXTRA_PLAYLIST) ?: "")
      TAG_CHANGE -> {
        val newSong = intent.getSerializableExtra(BaseMusicActivity.EXTRA_NEW_SONG) as Song?
        val oldSong = intent.getSerializableExtra(BaseMusicActivity.EXTRA_OLD_SONG) as Song?
        if (newSong != null && oldSong != null) {
          onTagChanged(oldSong, newSong)
        }
      }
    }
  }

  private fun handleMetaChange() {
    if (playQueue.song == EMPTY_SONG) {
      return
    }
    updateAppwidget()
    updateNotification()
    updateMediaSession(operation)
    // 是否需要保存进度
    if (playAtBreakPoint) {
      startSaveProgress()
    }

    sendLocalBroadcast(Intent(META_CHANGE))
  }

  fun updateNotification() {
    notify.updateForPlaying()
  }

  fun updateNotificationWithLrc(lrc: String) {
    notify.updateWithLyric(lrc)
  }

  private fun handlePlayStateChange() {
    if (playQueue.song == EMPTY_SONG) {
      return
    }
    LyricsManager.isPlaying = isPlaying
    sendLocalBroadcast(Intent(PLAY_STATE_CHANGE))
  }

  /**
   * 接受控制命令 包括暂停、播放、上下首、改版播放模式等
   */
  private var lastCommandTime: Long = 0

  inner class ControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
      handleCommand(intent)
    }
  }

  private fun handleCommand(intent: Intent?) {
    Timber.v("handleCommand: %s", intent)
    if (intent == null || intent.extras == null) {
      return
    }
    val control = intent.getIntExtra(EXTRA_CONTROL, -1)
    this@MusicService.control = control
    Timber.v("control: $control")

    if (control == Command.PLAYSELECTEDSONG || control == Command.PREV || control == Command.NEXT
        || control == Command.TOGGLE || control == Command.PAUSE || control == Command.START) {
      //判断下间隔时间
      if ((control == Command.PREV || control == Command.NEXT) && System.currentTimeMillis() - lastCommandTime < 500) {
        Timber.v("间隔小于500ms")
        return
      }
      //保存控制命令,用于播放界面判断动画
      operation = control
      if (playQueue.originalQueue.isEmpty()) {
        //列表为空，尝试读取
        Timber.v("列表为空，尝试读取")
        launch(context = Dispatchers.IO) {
          playQueue.restoreIfNecessary()
        }
        return
      }
    }
    lastCommandTime = System.currentTimeMillis()

    when (control) {
      //关闭通知栏
      Command.CLOSE_NOTIFY -> {
        Notify.isNotifyShowing = false
        //          if (notify instanceof NotifyImpl24) { //仅仅只是设置标志位
        //            return;
        //          }
        pause(false)
        uiHandler.postDelayed({ notify.cancelPlayingNotify() }, 300)
      }
      //播放选中的歌曲
      Command.PLAYSELECTEDSONG -> {
        playSelectSong(intent.getIntExtra(EXTRA_POSITION, -1))
      }
      //播放上一首
      Command.PREV -> {
        playPrevious()
      }
      //播放下一首
      Command.NEXT -> {
        playNext()
      }
      //暂停或者继续播放
      Command.TOGGLE -> {
        toggle()
      }
      //暂停
      Command.PAUSE -> {
        pause(false)
      }
      //继续播放
      Command.START -> {
        play(false)
      }
      //改变播放模式
      Command.CHANGE_MODEL -> {
        playModel = if (playModel == MODE_REPEAT) MODE_LOOP else playModel + 1
      }
      //取消或者添加收藏
      Command.LOVE -> {
        repository.toggleMyLove(playQueue.song.id)
            .compose(applySingleScheduler())
            .subscribe({
              if (it) {
                isLove = !isLove
                updateAppwidget()
              }
            }, {
              Timber.v(it)
            })
      }
      //桌面歌词
      Command.TOGGLE_DESKTOP_LYRIC -> {
        LyricsManager.setDesktopLyricsEnabled(!LyricsManager.isDesktopLyricsEnabled)
      }
      //临时播放一首歌曲
      Command.PLAY_TEMP -> {
        intent.getSerializableExtra(EXTRA_SONG)?.let {
          operation = Command.PLAY_TEMP
          playQueue.song = it as Song.Local
          prepare(playQueue.song)
        }
      }
      //解锁桌面歌词
      Command.UNLOCK_DESKTOP_LYRIC -> {
        LyricsManager.isDesktopLyricsLocked = false
      }
      //某一首歌曲添加至下一首播放
      Command.ADD_TO_NEXT_SONG -> {
        val nextSong = intent.getSerializableExtra(EXTRA_SONG) as Song? ?: return
        //添加到播放队列
        playQueue.addNextSong(nextSong)
        ToastUtil.show(service, R.string.already_add_to_next_song)
      }
      //切换定时器
      Command.TOGGLE_TIMER -> {
        val hasDefault = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, false)
        if (!hasDefault) {
          ToastUtil.show(service, getString(R.string.plz_set_default_time))
        }
        val time = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, -1)
        SleepTimer.toggleTimer((time * 1000).toLong())
      }
      else -> {
        Timber.v("unknown command")
      }
    }
  }

  /**
   * 是否只需要更新播放状态,比如暂停
   */
  private fun updatePlayStateOnly(cmd: Int): Boolean {
    return cmd == Command.PAUSE || cmd == Command.START || cmd == Command.TOGGLE
  }

  /**
   * 清除锁屏显示的内容
   */
  private fun clearMediaSession() {
    mediaSession.setMetadata(MediaMetadataCompat.Builder().build())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mediaSession.setPlaybackState(
          PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0, 1f).build())
    } else {
      mediaSession.setPlaybackState(PlaybackStateCompat.Builder().build())
    }
  }


  /**
   * 更新锁屏
   */
  private fun updateMediaSession(control: Int) {
    val currentSong = playQueue.song
    if (currentSong == EMPTY_SONG || lockScreen == CLOSE_LOCKSCREEN) {
      return
    }

    val builder = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentSong.id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.artist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (playQueue.position + 1).toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playQueue.size().toLong())
    }

    mediaSession.setMetadata(builder.build())

    if (!updatePlayStateOnly(control)) {
      val placeholder = if (ThemeStore.isLightTheme) R.drawable.album_empty_bg_day else R.drawable.album_empty_bg_night
      Glide.with(this)
          .asBitmap()
          .load(currentSong)
          .error(placeholder)
          .centerCrop()
          .override(DensityUtil.dip2px(160f), DensityUtil.dip2px(160f))
          .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
              setMediaSessionData(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
              setMediaSessionData((errorDrawable as? BitmapDrawable)?.bitmap)
            }

            private fun setMediaSessionData(result: Bitmap?) {
              val bitmap = copy(result)
              builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
              mediaSession.setMetadata(builder.build())
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
          })
    }
    updatePlaybackState()
  }


  fun updatePlaybackState() {
    val desktopLyricLock = LyricsManager.isDesktopLyricsLocked

    val builder = PlaybackStateCompat.Builder()
    builder.setActiveQueueItemId(currentSong.id)
      .setState(if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, progress.toLong(), speed)
      .setActions(MEDIA_SESSION_ACTIONS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      builder.addCustomAction(
        PlaybackStateCompat.CustomAction.Builder(
          if (desktopLyricLock) ACTION_UNLOCK_DESKTOP_LYRIC else ACTION_TOGGLE_DESKTOP_LYRIC,
          getString(if (desktopLyricLock) R.string.desktop_lyric__unlock else R.string.desktop_lyric_lock),
          if (desktopLyricLock) R.drawable.ic_lock_open_black_24dp else R.drawable.ic_desktop_lyric_black_24dp
        ).build()
      )
    }
    mediaSession.setPlaybackState(builder.build())
  }

  /**
   * 准备播放
   *
   * @param song 播放歌曲的路径
   */
  private fun prepare(song: Song, requestFocus: Boolean = true) {
    LyricsManager.updateLyrics(song, null)
    tryLaunch(
        block = {
          Timber.v("prepare start: %s", song)
          if (TextUtils.isEmpty(song.data)) {
            ToastUtil.show(service, getString(R.string.path_empty))
            return@tryLaunch
          }

          if (requestFocus) {
            audioFocus = AudioManagerCompat.requestAudioFocus(
                audioManager,
                focusRequest
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!audioFocus) {
              ToastUtil.show(service, getString(R.string.cant_request_audio_focus))
              return@tryLaunch
            }
          }

          if (isPlaying) {
            mediaPlayer.pause()
          }
          prepared = false
          mediaPlayer.reset()
          setDataSource(song)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(speed)
          }
          mediaPlayer.prepareAsync()
          isLove = song.isLocal() && withContext(Dispatchers.IO) {
            repository.isMyLove(playQueue.song.id)
              .onErrorReturn {
                false
              }
              .blockingGet()
          }
          Timber.v("prepare finish: $song")
        },
        catch = {
          ToastUtil.show(service, getString(R.string.play_failed) + it.toString())
          prepared = false
        })
  }

  private suspend fun setDataSource(song: Song) = withContext(Dispatchers.IO) {
    Timber.v("setDataSource: ${song.data}")
    if (song.isLocal()) {
      mediaPlayer.setDataSource(this@MusicService, song.contentUri)
    } else if (song is Song.Remote) {
      val start = System.currentTimeMillis()
      mediaPlayer.setDataSource(this@MusicService, song.contentUri, song.headers)
      Timber.v("setDataSource spend: ${System.currentTimeMillis() - start}")

      // 可能会特别耗时
      launch(Dispatchers.IO) {
        retrieveRemoteSong(song, playQueue.song as Song.Remote)
      }
    }
  }

  /**
   * 根据当前播放模式，切换到上一首或者下一首
   *
   * @param isNext 是否是播放下一首
   */
  private var lastSwitchTime = 0L
  fun playNextOrPrev(isNext: Boolean) {
    if (System.currentTimeMillis() - lastSwitchTime <= 800) {
      return
    }
    lastSwitchTime = System.currentTimeMillis()
    if (playQueue.size() == 0) {
      ToastUtil.show(service, getString(R.string.list_is_empty))
      return
    }
    Timber.v("播放下一首")
    if (isNext) {
      playQueue.next()
    } else {
      playQueue.previous()
    }

    if (playQueue.song == EMPTY_SONG) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    // 不能在这里设置playing，因为远程的歌曲可能需要缓冲，并且这里需要提前刷新下界面
//    if (playQueue.song.isRemote()) {
//      uiHandler.sendEmptyMessage(UPDATE_META_DATA)
//    }

    prepare(playQueue.song)
  }

  /**
   * 设置MediaPlayer播放进度
   */
  fun setProgress(current: Long) {
    if (prepared) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        mediaPlayer.seekTo(current, MediaPlayer.SEEK_PREVIOUS_SYNC)
      } else {
        mediaPlayer.seekTo(current.toInt())
      }
      launch(Dispatchers.IO) {
        LyricsManager.updateProgress()
      }
      updatePlaybackState()
    }
  }

  fun setSpeed(speed: Float) {
    if (prepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer.isPlaying) {
      try {
        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
      } catch (e: Exception) {
        Timber.w(e)
      }
    }
  }

  /**
   * 读取歌曲id列表与播放队列
   */
  private fun loadSync() {
    launch(context = Dispatchers.IO) {
      load()
    }
  }

  @WorkerThread
  @Synchronized
  private fun load() {
    if (load >= LOADING || !hasPermission) {
      return
    }
    Timber.v("load")
    load = LOADING
    val isFirst = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, true)
    SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, false)
    //第一次启动软件
    if (isFirst) {
      //新建我的收藏
      repository.insertPlayList(getString(R.string.my_favorite)).subscribe(LogObserver())

      //通知栏样式
      SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC,
          Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
    }

    //摇一摇
    if (SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SHAKE, false)) {
      ShakeDetector.getInstance().beginListen()
    }

    //用户设置
    lockScreen = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, APLAYER_LOCKSCREEN)
    playModel = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL, MODE_LOOP)
    speed = java.lang.Float.parseFloat(SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SPEED, "1.0"))
    playAtBreakPoint = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.PLAY_AT_BREAKPOINT, false)
    lastProgress = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_PLAY_PROGRESS, 0)
    crossFade = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CROSS_FADE, true)

    //读取播放列表
    playQueue.restoreIfNecessary()
    prepare(playQueue.song, false)

    load = LOAD_SUCCESS

    uiHandler.postDelayed({ sendLocalBroadcast(Intent(META_CHANGE)) }, 400)
  }

  fun deleteSongFromService(deleteSongs: List<Song>?) {
    if (deleteSongs != null && deleteSongs.isNotEmpty()) {
      playQueue.removeAll(deleteSongs)
    }
  }

  /**
   * 释放电源锁
   */
  private fun releaseWakeLock() {
    if (wakeLock.isHeld) {
      wakeLock.release()
    }
  }

  /**
   * 获得电源锁
   */
  private fun acquireWakeLock() {
    wakeLock.acquire(if (playQueue.song != EMPTY_SONG) playQueue.song.duration else 30000L)
  }

  /**
   * 更新桌面部件
   */
  private fun updateAppwidget() {
    //暂停停止更新进度条和时间
    if (!isPlaying) {
      //暂停后不再更新
      //所以需要在停止前更新一次 保证桌面部件控件的播放|暂停按钮状态是对的
      desktopWidgetTask?.run()
      stopUpdateAppWidget()
    } else {
      if (screenOn) {
        appWidgets.forEach {
          it.value.updateWidget(this, null, true)
        }
        //开始播放后更新进度条和时间
        startUpdateAppWidget()
      }
    }
  }

  private fun stopUpdateAppWidget() {
    desktopWidgetTask?.cancel()
    desktopWidgetTask = null
  }

  private fun startUpdateAppWidget() {
    if (desktopWidgetTask != null) {
      return
    }
    desktopWidgetTask = WidgetTask()
    timer.schedule(desktopWidgetTask, INTERVAL_APPWIDGET, INTERVAL_APPWIDGET)
  }

  private inner class WidgetTask : TimerTask() {
    private val tag: String = WidgetTask::class.java.simpleName

    override fun run() {
      val isAppOnForeground = isAppOnForeground
      // app在前台不用更新
      if (!isAppOnForeground) {
        appWidgets.forEach {
          uiHandler.post {
            it.value.partiallyUpdateWidget(service)
          }
        }
      }
    }

    override fun cancel(): Boolean {
      Timber.tag(tag).v("停止更新桌面部件")
      return super.cancel()
    }
  }

  private fun startSaveProgress() {
    if (progressTask != null) {
      return
    }
    progressTask = ProgressTask()
    timer.schedule(progressTask, 1000, SAVE_PROGRESS_INTERVAL)
  }

  private fun stopSaveProgress() {
    progressTask?.cancel()
    progressTask = null
  }

  /**
   * 保存当前播放的进度
   */
  private inner class ProgressTask : TimerTask() {
    override fun run() {
      val progress = progress
      if (progress > 0) {
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_PLAY_PROGRESS, progress)
      }
    }
  }

  private inner class AudioFocusChangeListener : AudioManager.OnAudioFocusChangeListener {

    //记录焦点变化之前是否在播放;
    private var needContinue = false

    override fun onAudioFocusChange(focusChange: Int) {
      when (focusChange) {
        //获得AudioFocus
        AudioManager.AUDIOFOCUS_GAIN -> {
          audioFocus = true
          if (!prepared) {
            setUpPlayer()
          } else if (needContinue) {
            play(true)
            needContinue = false
            operation = Command.TOGGLE
          }
          volumeController.directTo(1f)
        }
        //短暂暂停
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          needContinue = isPlaying
          if (isPlaying && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
        //减小音量
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          volumeController.directTo(.1f)
        }
        //暂停
        AudioManager.AUDIOFOCUS_LOSS -> {
          val ignoreFocus = SPUtil.getValue(this@MusicService, SETTING_KEY.NAME, SETTING_KEY.AUDIO_FOCUS, false)
          if (ignoreFocus) {
            Timber.v("忽略音频焦点 不暂停")
            return
          }
          audioFocus = false
          if (isPlaying && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
      }
    }
  }


  private class PlaybackHandler(
      service: MusicService,
      private val ref: WeakReference<MusicService> = WeakReference(service))
    : Handler() {

    override fun handleMessage(msg: Message) {
      if (ref.get() == null) {
        return
      }
      val musicService = ref.get() ?: return
      when (msg.what) {
        UPDATE_PLAY_STATE -> musicService.handlePlayStateChange()
        UPDATE_META_DATA -> {
//          musicService.handlePlayStateChange()
          musicService.handleMetaChange()
        }
        UPDATE_NOTIFICATION -> {
          musicService.updateNotification()
        }
      }
    }
  }

  private inner class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      Timber.tag("ScreenReceiver").v(action)
      if (Intent.ACTION_SCREEN_ON == action) {
        screenOn = true
        //显示锁屏
        if (isPlaying && SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, APLAYER_LOCKSCREEN) == APLAYER_LOCKSCREEN) {
          try {
            context.startActivity(Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
          } catch (e: Exception) {
            Timber.v("启动锁屏页失败: $e")
          }
        }
        //重新开始更新桌面部件
        updateAppwidget()
      } else {
        screenOn = false
        //停止更新桌面部件
        stopUpdateAppWidget()
      }
    }
  }

  companion object {
    const val TAG_LIFECYCLE = "ServiceLifeCycle"
    const val EXTRA_SONG = "Song"
    const val EXTRA_POSITION = "Position"

    //更新桌面部件
    const val UPDATE_APPWIDGET = 1000

    //更新正在播放歌曲
    const val UPDATE_META_DATA = 1002

    //更新播放状态
    const val UPDATE_PLAY_STATE = 1003

    //更新通知
    const val UPDATE_NOTIFICATION = 1008

    private const val APLAYER_PACKAGE_NAME = "remix.myplayer"

    //媒体数据库变化
    const val MEDIA_STORE_CHANGE = "$APLAYER_PACKAGE_NAME.media_store.change"

    //读写权限变化
    const val PERMISSION_CHANGE = "$APLAYER_PACKAGE_NAME.permission.change"

    //播放列表变换
    const val PLAYLIST_CHANGE = "$APLAYER_PACKAGE_NAME.playlist.change"

    //播放数据变化
    const val META_CHANGE = "$APLAYER_PACKAGE_NAME.meta.change"

    //播放状态变化
    const val PLAY_STATE_CHANGE = "$APLAYER_PACKAGE_NAME.play_state.change"

    //歌曲标签变化
    const val TAG_CHANGE = "$APLAYER_PACKAGE_NAME.tag_change"

    const val EXTRA_CONTROL = "Control"
    const val EXTRA_SHUFFLE = "shuffle"
    const val ACTION_APPWIDGET_OPERATE = "$APLAYER_PACKAGE_NAME.appwidget.operate"
    const val ACTION_SHORTCUT_SHUFFLE = "$APLAYER_PACKAGE_NAME.shortcut.shuffle"
    const val ACTION_SHORTCUT_MYLOVE = "$APLAYER_PACKAGE_NAME.shortcut.my_love"
    const val ACTION_SHORTCUT_LASTADDED = "$APLAYER_PACKAGE_NAME.shortcut.last_added"
    const val ACTION_LOAD_FINISH = "$APLAYER_PACKAGE_NAME.load.finish"
    const val ACTION_CMD = "$APLAYER_PACKAGE_NAME.cmd"
    const val ACTION_WIDGET_UPDATE = "$APLAYER_PACKAGE_NAME.widget_update"
    const val ACTION_TOGGLE_TIMER = "$APLAYER_PACKAGE_NAME.toggle_timer"
    const val ACTION_UNLOCK_DESKTOP_LYRIC = "$APLAYER_PACKAGE_NAME.unlock.desktop_lyric"
    const val ACTION_TOGGLE_DESKTOP_LYRIC = "$APLAYER_PACKAGE_NAME.toggle.desktop_lyric"

    private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
        or PlaybackStateCompat.ACTION_PAUSE
        or PlaybackStateCompat.ACTION_PLAY_PAUSE
        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        or PlaybackStateCompat.ACTION_STOP
        or PlaybackStateCompat.ACTION_SEEK_TO)

    private const val APPWIDGET_BIG = "AppWidgetBig"
    private const val APPWIDGET_MEDIUM = "AppWidgetMedium"
    private const val APPWIDGET_SMALL = "AppWidgetSmall"
    private const val APPWIDGET_MEDIUM_TRANSPARENT = "AppWidgetMediumTransparent"
    private const val APPWIDGET_SMALL_TRANSPARENT = "AppWidgetSmallTransparent"

    private const val INTERVAL_APPWIDGET = 1000L
    private const val SAVE_PROGRESS_INTERVAL = 500L
    private val UPDATE_LYRICS_INTERVAL = 50.milliseconds // TODO: 是否合适？


    /**
     * 复制bitmap
     */
    @JvmStatic
    fun copy(bitmap: Bitmap?): Bitmap? {
      if (bitmap == null || bitmap.isRecycled) {
        return null
      }
      val config: Bitmap.Config = bitmap.config
      return try {
        bitmap.copy(config, false)
      } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        null
      }

    }

    fun retrieveRemoteSong(song: Song.Remote, targetSong: Song.Remote) {
      Timber.v("retrieveRemoteSong: ${song.data}")
      val start = System.currentTimeMillis()
      val metadataRetriever = MediaMetadataRetriever()
      try {
        metadataRetriever.setDataSource(song.data, song.headers)
        val title =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?: song.title
        val album =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
        val artist =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val duration =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLong() ?: 0L
        val year =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: ""
        val genre =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""
        val track =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS) ?: ""
        val dateModified = if (song.dateModified > 0) {
          song.dateModified
        } else {
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            ?.toLongOrNull() ?: 0
        }
        targetSong.bitRate =
          metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          targetSong.sampleRate =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
              ?: ""
        }

        targetSong.updateMetaData(
          title,
          album,
          artist,
          duration,
          year,
          genre,
          track,
          dateModified
        )
      } catch (e: Exception) {
        Timber.v("fail to retrieveRemoteSong data: ${song.data} detail: $e")
      } finally {
        Timber.v("retrieveRemoteSong spend:${System.currentTimeMillis() - start} ${song.data}")
        metadataRetriever.release()
      }
    }
  }
}

