package remix.myplayer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.App
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.Song.Companion.EMPTY_SONG
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.LOCKSCREEN_APLAYER
import remix.myplayer.data.prefs.SettingPrefs.Companion.LOCKSCREEN_CLOSE
import remix.myplayer.data.prefs.SettingPrefs.Companion.LOCKSCREEN_SYSTEM
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_LOOP
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_REPEAT
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_SHUFFLE
import remix.myplayer.data.prefs.SettingPrefs.Companion.OPEN_SOFTWARE
import remix.myplayer.lyric.LyricManager
import remix.myplayer.misc.checkMainThread
import remix.myplayer.misc.getPendingIntentFlag
import remix.myplayer.misc.helper.EQHelper
import remix.myplayer.misc.helper.LanguageHelper
import remix.myplayer.misc.helper.MusicEventCallback
import remix.myplayer.misc.helper.ShakeDetector
import remix.myplayer.misc.helper.SleepTimer
import remix.myplayer.misc.observer.MediaStoreObserver
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.misc.receiver.MediaButtonReceiver
import remix.myplayer.misc.tryLaunch
import remix.myplayer.repo.HistoryRepository
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.usecase.FetchMetaDataUseCase
import remix.myplayer.service.notification.Notify
import remix.myplayer.service.notification.NotifyImpl
import remix.myplayer.service.notification.NotifyImpl24
import remix.myplayer.service.playback.ExoPlayback
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.service.playback.Playback
import remix.myplayer.ui.activity.LockScreenActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PERMISSION
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import remix.myplayer.ui.appwidgets.BaseAppwidget
import remix.myplayer.ui.appwidgets.big.AppWidgetBig
import remix.myplayer.ui.appwidgets.medium.AppWidgetMedium
import remix.myplayer.ui.appwidgets.medium.AppWidgetMediumTransparent
import remix.myplayer.ui.appwidgets.small.AppWidgetSmall
import remix.myplayer.ui.appwidgets.small.AppWidgetSmallTransparent
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.ThemeController
import remix.myplayer.util.Constants.ACTION_EXIT
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.isAppOnForeground
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放Service 歌曲的播放 控制 回调相关activity的界面更新 通知栏的控制
 */
@SuppressLint("CheckResult")
@AndroidEntryPoint
class MusicService : BaseService(),
  AudioFocusManager.Callbacks,
  Playback.PlayerCallback,
  MusicEventCallback,
  SharedPreferences.OnSharedPreferenceChangeListener,
  CoroutineScope by MainScope() {

  @Inject
  lateinit var audioFocusManager: AudioFocusManager

  @Inject
  lateinit var themeController: ThemeController

  @Inject
  lateinit var lyricManager: LyricManager

  @Inject
  lateinit var settingPrefs: SettingPrefs

  @Inject
  lateinit var playQueue: PlayQueue

  @Inject
  lateinit var playListRepository: PlayListRepository

  @Inject
  lateinit var songRepository: SongRepository

  @Inject
  lateinit var historyRepository: HistoryRepository

  @Inject
  lateinit var fetchMetaDataUseCase: FetchMetaDataUseCase

  private val stateSource = MusicStateSource

  private val playbackState
    get() = stateSource.playbackUiState.value
  private val progressState
    get() = stateSource.progressState.value

  /**
   * 是否第一次准备完成
   */
  private var firstPrepared = true

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
  private var playModel: Int = MODE_LOOP
    set(value) {
      Timber.v("修改播放模式: $value")
      settingPrefs.playModel = value

      field = value
      playback.setMode(value)
      partiallyUpdateWidget()

      updateQueueItem()

      pushPlaybackUiState()
    }
    get() = settingPrefs.playModel

  /**
   * 播放完当前歌曲后是否停止app
   */
  private var pendingClose: Boolean = false

  /**
   * MediaPlayer 负责歌曲的播放等
   */
  lateinit var playback: ExoPlayback
    private set

  /**
   * 桌面部件
   */
  private val appWidgets: HashMap<String, BaseAppwidget> = HashMap()

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
   * MediaSession
   */
  lateinit var mediaSession: MediaSessionCompat
    private set

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
      if (field == value) {
        return
      }
      field = value
      lyricManager.isScreenOn = value
    }

  /**
   * 音量控制
   */
  private val volumeController: VolumeController by lazy {
    VolumeController(this)
  }

  /**
   * 保存播放进度
   */
  private var progressJob: Job? = null

  /**
   * 更新桌面组件
   */
  private var desktopWidgetJob: Job? = null

  /**
   * 准备歌曲
   */
  private var prepareJob: Job? = null

  /**
   * 操作类型
   */
  private var lastOp: Int = -1
    set(value) {
      field = value
      pushPlaybackUiState()
    }

  /**
   * Binder
   */
  private val musicBinder = MusicBinder()

  /**
   * 监听MediaStore变化
   */
  private val mediaStoreObserver: MediaStoreObserver by lazy {
    MediaStoreObserver()
  }
  private lateinit var service: MusicService

  private var hasPermission = false

  private var alreadyUnInit: Boolean = false

  /**
   * 当前是否正在播放
   */
  private val isPlaying: Boolean
    get() = playbackState.isPlaying

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

  override fun onBind(intent: Intent): IBinder {
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
      load()
      delay(200)
      handleStartCommandIntent(commandIntent, action)
    }
    return START_NOT_STICKY
  }

  override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
//    Timber.v("onSharedPreferenceChanged, key: $key")
    when (key) {
      // 通知栏样式
      PrefKeys.Setting.NOTIFY_STYLE_CLASSIC -> {
        val wasShowing = notify.isNotifyShowing
        notify = if (settingPrefs.classicNotify) {
          NotifyImpl(this@MusicService)
        } else {
          NotifyImpl24(this@MusicService)
        }
        if (wasShowing) {
          // 先取消再重新显示 让通知栏彻底刷新一次
          notify.cancelPlayingNotify()
          updateNotification()
        }
      }
      // 锁屏
      PrefKeys.Setting.LOCKSCREEN -> {
        when (settingPrefs.lockScreen) {
          LOCKSCREEN_CLOSE -> clearMediaSession()
          LOCKSCREEN_SYSTEM, LOCKSCREEN_APLAYER -> updateMediaSession(Command.SKIP_TO_NEXT)
        }
      }
      // 断点播放
      PrefKeys.Setting.PLAY_AT_BREAKPOINT -> {
        if (!settingPrefs.playAtBreakPoint) {
          stopSaveProgress()
        } else {
          startSaveProgress()
        }
      }
      // 倍速播放
      PrefKeys.Setting.SPEED -> {
        playback.speed = settingPrefs.speedValue
        pushPlaybackUiState()
      }
    }
  }

  private fun setUp() {
    // 配置变化
    getSharedPreferences(PrefKeys.Setting.NAME, MODE_PRIVATE)
      .registerOnSharedPreferenceChangeListener(this)

    // 通知栏
    settingPrefs.classicNotify
    notify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !settingPrefs.classicNotify) {
      NotifyImpl24(this)
    } else {
      NotifyImpl(this)
    }

    // 桌面部件
    appWidgets[APPWIDGET_BIG] = AppWidgetBig.getInstance()
    appWidgets[APPWIDGET_MEDIUM] = AppWidgetMedium.getInstance()
    appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] = AppWidgetMediumTransparent.getInstance()
    appWidgets[APPWIDGET_SMALL] = AppWidgetSmall.getInstance()
    appWidgets[APPWIDGET_SMALL_TRANSPARENT] = AppWidgetSmallTransparent.getInstance()

    // 初始化Receiver
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

    // 监听数据库变化
    contentResolver.registerContentObserver(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      true,
      mediaStoreObserver
    )

    // 定时关闭
    SleepTimer.addCallback(object : SleepTimer.Callback {
      override fun onFinish() {
        if (settingPrefs.exitAfterTimerFinish) {
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

    lyricManager.isServiceAvailable = true

    audioFocusManager.attach(this)
    setUpPlayback()
    setUpSession()

    launch {
      // 收集播放状态
      stateSource.playbackUiState.collect {
        Timber.v("collect, playbackState collect: $it")
        handleMetaChange()
      }
    }
  }

  /**
   * 初始化mediaSession
   */
  private fun setUpSession() {
    val mediaButtonReceiverComponentName = ComponentName(
      applicationContext,
      MediaButtonReceiver::class.java
    )

    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
    mediaButtonIntent.component = mediaButtonReceiverComponentName

    val pendingIntent =
      PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, getPendingIntentFlag())

    mediaSession = MediaSessionCompat(
      applicationContext,
      "APlayer",
      mediaButtonReceiverComponentName,
      pendingIntent
    )
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
      override fun onMediaButtonEvent(event: Intent): Boolean {
        return MediaButtonReceiver.handleMediaButtonIntent(this@MusicService, event)
      }

      override fun onSkipToNext() {
        Timber.v("onSkipToNext")
        skipToNext()
      }

      override fun onSkipToPrevious() {
        Timber.v("onSkipToPrevious")
        skipToPrevious()
      }

      override fun onPlay() {
        Timber.v("onPlay")
        start(true)
      }

      override fun onPause() {
        Timber.v("onPause")
        pause()
      }

      override fun onStop() {
        pause()
        notify.cancelPlayingNotify()
        stopSelf()
      }

      override fun onSeekTo(pos: Long) {
        seekTo(pos)
      }

      override fun onCustomAction(action: String?, extras: Bundle?) {
        Timber.v("onCustomAction, ac: $action extra: $extras")
        when (action) {
          ACTION_UNLOCK_DESKTOP_LYRIC -> lyricManager.isDesktopLyricLocked = false
          ACTION_TOGGLE_DESKTOP_LYRIC -> lyricManager.setDesktopLyricEnabled(!lyricManager.isDesktopLyricEnabled)
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
  private fun setUpPlayback() {
    playback = ExoPlayback(this)
    playback.attach(this)

    EQHelper.init(this, playback.audioSessionId)
    EQHelper.open(this, playback.audioSessionId)
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    Timber.v("onIsPlayingChanged: $isPlaying")
    stateSource.updatePlaybackUiState(isPlaying = isPlaying)
    if (isPlaying) {
      updatePlayHistory()
    }
  }

  override fun onPrepare() {
    Timber.v("onPrepare, firstPrepared: $firstPrepared")

    pushPlaybackUiState()

    if (firstPrepared) {
      firstPrepared = false

      // 自动播放策略
      if (settingPrefs.autoPlay != OPEN_SOFTWARE) {
        return
      }
    }

    Timber.v("开始播放")
    // 开始播放
    start(false)
  }

  override fun onEnded() {
    Timber.v("onEnded")
    // 理论上应该不会到这?
    if (BuildConfig.DEBUG) {
      throw IllegalStateException("onEnded")
    }
  }

  override fun onItemTransition(mediaItem: MediaItem?, reason: Int) {
    Timber.v("onItemTransition, id: ${mediaItem?.mediaId} reason: $reason playing: ${isPlaying} currentSong: ${playback.currentSong?.title}")

    val song = mediaItem?.localConfiguration?.tag as? Song
    if (song is Song.Remote) {
      launch {
        withContext(Dispatchers.IO) {
          fetchMetaDataUseCase(song)
        }
        pushPlaybackUiState()
      }
    }

    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
      if (pendingClose) {
        Timber.v("发送Exit广播")
        sendBroadcast(
          Intent(ACTION_EXIT)
            .setComponent(ComponentName(this@MusicService, ExitReceiver::class.java))
        )
        return
      }

      if (playModel == MODE_REPEAT) {
        lastOp = Command.PLAY
      } else {
        lastOp = Command.SKIP_TO_NEXT
      }
    }

    if (isPlaying && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
      updatePlayHistory(song, checkDuplicate = false)
    }

    pushPlaybackUiState()
  }

  override fun onError(error: PlaybackException) {
    Timber.e("onPlayerError, code: ${error.errorCode} name: ${error.errorCodeName} cause: ${error.cause}")
    MessageNotifier.show(R.string.play_failed, error.errorCodeName)
    when (error.errorCode) {
      // fatal error
      PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
      PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
      PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
      PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> {
        prepareJob?.cancel()
        playback.release()
        setUpPlayback()
        launch {
          restorePlayList()
        }
      }
    }
  }

  override fun onPositionChange() {
    pushProgressUiState()
  }

  /**
   * 更新播放历史：只在歌曲真实开始播放时写入
   */
  private fun updatePlayHistory(
    song: Song? = playback.currentSong,
    checkDuplicate: Boolean = true
  ) {
    checkMainThread()
    Timber.v("updatePlayHistory, song: ${song?.title}")
    val songId = song?.takeIf { it.isLocal() }?.id ?: return
    launch {
      historyRepository.update(songId, checkDuplicate)
    }
  }

  private fun unInit() {
    if (alreadyUnInit) {
      return
    }

    cancel()

    EQHelper.close(this, playback.audioSessionId)
    if (isPlaying) {
      pause()
    }
    playback.release()
    load = 0

    notify.cancelPlayingNotify()

    lyricManager.isServiceAvailable = false

    updateNotification()

    audioFocusManager.detach()

    mediaSession.isActive = false
    mediaSession.release()

    unregisterLocalReceiver(controlReceiver)
    unregisterLocalReceiver(musicEventReceiver)
    unregisterLocalReceiver(widgetReceiver)
    Util.unregisterReceiver(this, headSetReceiver)
    Util.unregisterReceiver(this, screenReceiver)

    getSharedPreferences(PrefKeys.Setting.NAME, MODE_PRIVATE)
      .unregisterOnSharedPreferenceChangeListener(this)

    contentResolver.unregisterContentObserver(mediaStoreObserver)

    ShakeDetector.getInstance().stopListen()

    alreadyUnInit = true
  }

  private fun updateQueueItem() {
    Timber.v("updateQueueItem")
    val playlist = playback.getPlaylist()
    val title = playback.currentSong?.title
    tryLaunch(block = {
      val queue = withContext(Dispatchers.Default) {
        ArrayList(playlist)
          .map { song ->
            return@map MediaSessionCompat.QueueItem(
              MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .build().description, song.id
            )
          }
      }
      Timber.v("updateQueueItem, queue: ${queue.size}")
      mediaSession.setQueueTitle(title)
      mediaSession.setQueue(queue)
    }, catch = {
      MessageNotifier.show(it.toString())
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
    if (newQueue == playback.getPlaylist()) {
      return
    }

    playback.setPlaylist(newQueue)
    updateQueueItem()
    launch { playQueue.save(newQueue) }
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
    val equals = newQueue == playback.getPlaylist()
    if (!equals) {
      playback.setPlaylist(newQueue)
      launch { playQueue.save(newQueue) }
    }
    if (shuffle) {
      playModel = MODE_SHUFFLE
    }
    handleCommand(intent)

    if (equals) {
      return
    }
    updateQueueItem()
  }

  /**
   * 从播放队列移除歌曲并保存
   */
  fun removeFromQueue(ids: List<Long>) {
    if (ids.isNotEmpty()) {
      val idSet = ids.toSet()
      val playlist = playback.getPlaylist()
      val indices = ArrayList<Int>()
      playlist.forEachIndexed { index, song ->
        if (idSet.contains(song.id)) {
          indices.add(index)
        }
      }

      if (indices.isNotEmpty()) {
        indices.sortDescending()
        indices.forEach { index ->
          playback.removeSong(index)
        }
        launch { playQueue.save(playback.getPlaylist()) }

        updateQueueItem()
        pushPlaybackUiState()
      }
    }
  }

  /**
   * 添加歌曲到播放队列并保存
   */
  fun insertToQueue(songs: List<Song>) {
    if (songs.isNotEmpty()) {
      playback.addSongs(songs)
      launch { playQueue.save(playback.getPlaylist()) }
      pushPlaybackUiState()
    }
  }

  /**
   * 播放下一首
   */
  private fun skipToNext() {
    playback.skipToNext()
    if (!playback.isPlaying) {
      playback.start()
    }
  }

  /**
   * 播放上一首
   */
  private fun skipToPrevious() {
    playback.skipToPrevious()
    if (!playback.isPlaying) {
      playback.start()
    }
  }

  /**
   * 播放
   */
  private fun start(crossFade: Boolean) {
    Timber.v("play: $crossFade")
    if (!playback.isPrepared) {
      MessageNotifier.show(R.string.buffering_wait)
      return
    }

    if (!audioFocusManager.requestFocus()) {
      return
    }

    // 播放
    playback.start()

    // 渐变
    if (crossFade && settingPrefs.crossFade) {
      volumeController.fadeIn()
    } else {
      volumeController.directTo(1f)
    }
  }

  /**
   * 推送播放状态更新
   */
  private fun pushPlaybackUiState(isFavorite: Boolean? = null) {
    MusicStateSource.updatePlaybackUiState(
      song = playback.currentSong ?: EMPTY_SONG,
      nextSong = playback.nextSong ?: EMPTY_SONG,
      isPlaying = playback.isPlaying,
      isFavorite = isFavorite,
      speed = settingPrefs.speedValue,
      playModel = playModel,
      lastOp = lastOp
    )
    onPositionChange()
  }

  /**
   * 推送进度更新
   */
  private fun pushProgressUiState() {
    val position = playback.position
    val duration = playback.duration
    val buffered = playback.bufferedPosition
    stateSource.updateProgressUiState(
      position = if (position == C.TIME_UNSET) 0 else position,
      duration = if (duration == C.TIME_UNSET) 0 else duration,
      buffered = if (buffered == C.TIME_UNSET) 0 else buffered
    )
  }

  /**
   * 根据当前播放状态暂停或者继续播放
   */
  private fun playPause() {
    Timber.v("playPause")
    if (playback.isPlaying) {
      pause()
    } else {
      start(true)
    }
  }

  /**
   * 暂停
   */
  private fun pause() {
    Timber.v("pause")
    // 如果当前已经暂停了 就不重复操作了 避免已经关闭了通知栏又再次显示
    if (!playback.isPlaying) {
      return
    }
    if (settingPrefs.crossFade) {
      volumeController.fadeOut()
    } else {
      playback.pause()
    }
  }

  /**
   * 播放选中的歌曲 比如在全部歌曲或者专辑详情里面选中某一首歌曲
   *
   * @param position 播放位置
   */
  private fun playAt(position: Int) {
    Timber.v("playAt, $position")

    if (position == -1 || position >= playback.mediaItemCount) {
      MessageNotifier.show(R.string.illegal_arg)
      return
    }

    playback.skipTo(position)
    if (!playback.isPlaying) {
      playback.start()
    }
  }

  override fun onMediaStoreChanged() {
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission && has) {
      hasPermission = true
      launch {
        load()
      }
    }
  }

  /**
   * 标签改变，比如编辑了标题等信息
   */
  override fun onTagChanged(oldSong: Song?, newSong: Song) {
    if (!newSong.isLocal() || !newSong.valid()) {
      return
    }

    playback.replaceSong(newSong)
    lyricManager.clearCache(newSong)
    lyricManager.updateLyrics(newSong)
    updateQueueItem()
    pushPlaybackUiState()
  }

  override fun onPlayListChanged(name: String) {
    Timber.v("onPlayListChanged: $name")
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
        handleCommand(
          Intent(ACTION_CMD).putExtra(
            EXTRA_CONTROL,
            commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
          )
        )
      }

      ACTION_SHORTCUT_SHUFFLE -> {
        if (playModel != MODE_SHUFFLE) {
          playModel = MODE_SHUFFLE
        }
        handleCommand(Intent(ACTION_CMD).putExtra(EXTRA_CONTROL, Command.SKIP_TO_NEXT))
      }

      ACTION_SHORTCUT_MYLOVE -> {
        tryLaunch {
          val playlist = playListRepository.getFavorite() ?: return@tryLaunch

          val songs =
            withContext(Dispatchers.IO) { songRepository.getSongsByModels(listOf(playlist)) }

          if (songs.isEmpty()) {
            MessageNotifier.show(R.string.list_is_empty)
            return@tryLaunch
          }

          setPlayQueue(songs, Intent(ACTION_CMD).apply {
            putExtra(EXTRA_CONTROL, Command.PLAY_AT)
            putExtra(EXTRA_POSITION, 0)
          })
        }

      }

      ACTION_SHORTCUT_LASTADDED -> {
        tryLaunch {
          val songs = withContext(Dispatchers.IO) {
            songRepository.getLastAddedSongs()
          }
          if (songs.isEmpty()) {
            MessageNotifier.show(R.string.list_is_empty)
            return@tryLaunch
          }
          val lastedIntent = Intent(ACTION_CMD)
          lastedIntent.putExtra(EXTRA_CONTROL, Command.PLAY_AT)
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
        if (newSong != null) {
          onTagChanged(oldSong, newSong)
        }
      }
    }
  }

  private fun handleMetaChange() {
    val song = playback.currentSong ?: EMPTY_SONG
    if (song == EMPTY_SONG) {
      return
    }
    updateAppwidget()

    // 正在播放、已有通知在显示、用户操作过
    if (isPlaying || notify.isNotifyShowing || lastOp != -1) {
      updateNotification()
    }
    updateMediaSession(lastOp)
    // 是否需要保存进度
    if (settingPrefs.playAtBreakPoint) {
      startSaveProgress()
    }
    // 保存当前播放歌曲
    settingPrefs.lastSong = if (song.isLocal()) song.id.toString() else song.data
  }

  fun updateNotification() {
    notify.updateForPlaying()
  }

  fun updateNotificationWithLrc(lrc: String) {
    notify.updateWithLyric(lrc)
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

    if (control == Command.PLAY_AT || control == Command.SKIP_TO_PREVIOUS || control == Command.SKIP_TO_NEXT
      || control == Command.PLAY_PAUSE || control == Command.PAUSE || control == Command.PLAY
    ) {
      // 判断下间隔时间
      if ((control == Command.SKIP_TO_PREVIOUS || control == Command.SKIP_TO_NEXT) && System.currentTimeMillis() - lastCommandTime < INTERVAL_CONTROL) {
        Timber.v("间隔小于500ms")
        return
      }
      // 保存控制命令,用于播放界面判断动画
      lastOp = control
      if (playback.mediaItemCount == 0) {
        // 列表为空，尝试读取
        Timber.v("列表为空，尝试读取")
        tryLaunch {
          load()
        }
        return
      }
    }
    lastCommandTime = System.currentTimeMillis()

    when (control) {
      // 关闭通知栏
      Command.CLOSE_NOTIFY -> {
        notify.isNotifyShowing = false
        pause()
        launch {
          delay(300)
          notify.cancelPlayingNotify()
        }
      }
      // 播放选中的歌曲
      Command.PLAY_AT -> {
        playAt(intent.getIntExtra(EXTRA_POSITION, -1))
      }
      // 播放上一首
      Command.SKIP_TO_PREVIOUS -> {
        skipToPrevious()
      }
      // 播放下一首
      Command.SKIP_TO_NEXT -> {
        skipToNext()
      }
      // 暂停或者继续播放
      Command.PLAY_PAUSE -> {
        playPause()
      }
      // 暂停
      Command.PAUSE -> {
        pause()
      }
      // 继续播放
      Command.PLAY -> {
        start(false)
      }
      // 改变播放模式
      Command.CHANGE_MODEL -> {
        playModel = if (playModel == MODE_REPEAT) MODE_LOOP else playModel + 1
      }
      // 取消或者添加收藏
      Command.LOVE -> {
        launch {
          playback.currentSong?.let {
            playListRepository.toggleFavorite(it.id)
            MusicStateSource.updatePlaybackUiState(isFavorite = !playbackState.isFavorite)
            updateAppwidget()
          }
        }
      }
      // 桌面歌词
      Command.TOGGLE_DESKTOP_LYRIC -> {
        lyricManager.setDesktopLyricEnabled(!lyricManager.isDesktopLyricEnabled)
      }
      // 临时播放一首歌曲
      Command.PLAY_TEMP -> {
        intent.getSerializableExtra(EXTRA_SONG)?.let {
          lastOp = Command.PLAY_TEMP
          val song = it as Song.Local

          if (playback.getPlaylist().isEmpty()) {
            playback.setPlaylist(listOf(song))
          } else if (playback.currentSong?.id != song.id) {
            playback.addToNextSong(song)
            skipToNext()
          } else {
            // 如果是当前歌曲，从头播放
            seekTo(0)
          }

          launch { playQueue.save(playback.getPlaylist()) }
          start(true)
        }
      }
      // 解锁桌面歌词
      Command.UNLOCK_DESKTOP_LYRIC -> {
        lyricManager.isDesktopLyricLocked = false
      }
      // 某一首歌曲添加至下一首播放
      Command.ADD_TO_NEXT_SONG -> {
        val nextSong = intent.getSerializableExtra(EXTRA_SONG) as Song? ?: return

        if (playback.addToNextSong(nextSong)) {
          // 同步更新
          launch { playQueue.save(playback.getPlaylist()) }
          pushPlaybackUiState()
          MessageNotifier.show(R.string.already_add_to_next_song)
        }

      }
      // 切换定时器
      Command.TOGGLE_TIMER -> {
        if (!settingPrefs.timerStartAuto) {
          MessageNotifier.show(R.string.plz_set_default_time)
        }
        SleepTimer.toggleTimer((settingPrefs.timerDefaultDuration * 1000).toLong())
      }
      // Seek
      Command.SEEK_TO -> {
        val pos = intent.getLongExtra(EXTRA_PROGRESS, -1L)
        if (pos >= 0L) {
          seekTo(pos)
        }
      }

      else -> {
        Timber.v("unknown command")
      }
    }
  }

  /**
   * 清除锁屏显示的内容
   */
  private fun clearMediaSession() {
    mediaSession.setMetadata(MediaMetadataCompat.Builder().build())
    mediaSession.setPlaybackState(
      PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0, 1f).build()
    )
  }

  /**
   * 更新锁屏
   */
  private fun updateMediaSession(control: Int) {
    val currentSong = playback.currentSong ?: EMPTY_SONG
    if (currentSong == EMPTY_SONG || settingPrefs.lockScreen == LOCKSCREEN_CLOSE) {
      return
    }

    val builder = MediaMetadataCompat.Builder()
      .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentSong.id.toString())
      .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.album)
      .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.artist)
      .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.artist)
      .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.duration)
      .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (playback.currentIndex + 1).toLong())
      .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
    builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playback.mediaItemCount.toLong())

    mediaSession.setMetadata(builder.build())
    updatePlaybackState()

    val placeholder =
      if (themeController.appTheme.isLight) R.drawable.album_empty_bg_day else R.drawable.album_empty_bg_night
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
          builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, copy(result))
          mediaSession.setMetadata(builder.build())
        }

        override fun onLoadCleared(placeholder: Drawable?) {

        }
      })
  }

  fun updatePlaybackState() {
    val desktopLyricLock = lyricManager.isDesktopLyricLocked

    val builder = PlaybackStateCompat.Builder()
    builder.setActiveQueueItemId(playbackState.song.id)
      .setState(
        if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
        progressState.position,
        playbackState.speed
      )
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
   * 设置播放进度
   */
  private fun seekTo(current: Long) {
    if (playback.isPrepared) {
      playback.seek(current)
      launch(Dispatchers.IO) {
        lyricManager.updateProgress()
      }
      updatePlaybackState()
    }
  }

  private suspend fun load() {
    if (load >= LOADING || !hasPermission) {
      return
    }
    Timber.v("load")
    load = LOADING
    // 第一次启动软件
    if (settingPrefs.firstLoad) {
      settingPrefs.firstLoad = false
      // 新建我的收藏
      playListRepository.insertPlayList(getString(R.string.my_favorite))

      // 通知栏样式
      settingPrefs.classicNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    }

    // 摇一摇
    if (settingPrefs.shake) {
      ShakeDetector.getInstance().beginListen()
    }

    restorePlayList()

    load = LOAD_SUCCESS
  }

  private suspend fun restorePlayList() {
    // 读取播放列表
    val (queue, pos) = withContext(Dispatchers.IO) {
      playQueue.restore()
    }

    if (queue.isNotEmpty()) {
      playback.setPlaylist(
        queue,
        pos,
        if (firstPrepared && settingPrefs.lastProgress > 0) settingPrefs.lastProgress.toLong() else 0L
      )
      playback.setMode(playModel)
    }
  }

  /**
   * 更新桌面部件
   */
  private fun updateAppwidget() {
    // 暂停停止更新进度条和时间
    if (!isPlaying) {
      // 暂停后不再更新
      // 所以需要在停止前更新一次 保证桌面部件控件的播放|暂停按钮状态是对的
      partiallyUpdateWidget(true)
      stopUpdateAppWidget()
    } else {
      if (screenOn) {
        appWidgets.forEach {
          it.value.updateWidget(this, null, true)
        }
        // 开始播放后更新进度条和时间
        startUpdateAppWidget()
      }
    }
  }

  private fun stopUpdateAppWidget() {
    desktopWidgetJob?.cancel()
    desktopWidgetJob = null
  }

  private fun startUpdateAppWidget() {
    if (desktopWidgetJob != null) {
      return
    }
    desktopWidgetJob = launch {
      while (isActive) {
        partiallyUpdateWidget()
        delay(INTERVAL_UPDATE_APPWIDGET)
      }

    }
  }

  private fun partiallyUpdateWidget(force: Boolean = false) {
    // app在前台不用更新
    if (!isAppOnForeground || force) {
      appWidgets.forEach {
        it.value.partiallyUpdateWidget(service)
      }
    }
  }

  private fun startSaveProgress() {
    if (progressJob != null) {
      return
    }
    progressJob = launch {
      while (isActive) {
        val progress = progressState.position
        if (progress > 0) {
          settingPrefs.lastProgress = progress.toInt()
        }

        delay(INTERVAL_SAVE_PROGRESS)
      }
    }
  }

  private fun stopSaveProgress() {
    progressJob?.cancel()
    progressJob = null
  }

  override fun onFocusGained() {
    Timber.v("onFocusGained")
    if (wasPlaying) {
      start(true)
      wasPlaying = false
      lastOp = Command.PLAY_PAUSE
    }
    volumeController.directTo(1f)
  }

  override fun onFocusLost() {
    Timber.v("onFocusLost")
    val ignoreFocus = settingPrefs.ignoreAudioFocus
    if (ignoreFocus) {
      Timber.v("忽略音频焦点 不暂停")
      return
    }
    if (isPlaying) {
      lastOp = Command.PLAY_PAUSE
      pause()
    }
  }

  private var wasPlaying = false
  override fun onFocusLostTransient() {
    Timber.v("onFocusLostTransient")
    wasPlaying = isPlaying
    if (isPlaying) {
      lastOp = Command.PLAY_PAUSE
      pause()
    }
  }

  override fun onFocusDuck() {
    Timber.v("onFocusDuck")
    if (isPlaying) {
      volumeController.directTo(.1f)
    }
  }

  private inner class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      Timber.tag("ScreenReceiver").v(action)
      if (Intent.ACTION_SCREEN_ON == action) {
        screenOn = true
        //显示锁屏
        if (isPlaying && settingPrefs.lockScreen == LOCKSCREEN_APLAYER) {
          try {
            context.startActivity(
              Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
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
    const val EXTRA_SONG = "song"
    const val EXTRA_POSITION = "position"

    private const val APLAYER_PACKAGE_NAME = "remix.myplayer"

    // 媒体数据库变化
    const val MEDIA_STORE_CHANGE = "$APLAYER_PACKAGE_NAME.media_store.change"

    // 读写权限变化
    const val PERMISSION_CHANGE = "$APLAYER_PACKAGE_NAME.permission.change"

    // 播放列表变换
    const val PLAYLIST_CHANGE = "$APLAYER_PACKAGE_NAME.playlist.change"

    // 歌曲标签变化
    const val TAG_CHANGE = "$APLAYER_PACKAGE_NAME.tag_change"

    const val EXTRA_CONTROL = "control"
    const val EXTRA_SHUFFLE = "shuffle"
    const val EXTRA_PROGRESS = "progress"
    const val ACTION_APPWIDGET_OPERATE = "$APLAYER_PACKAGE_NAME.appwidget.operate"
    const val ACTION_SHORTCUT_SHUFFLE = "$APLAYER_PACKAGE_NAME.shortcut.shuffle"
    const val ACTION_SHORTCUT_MYLOVE = "$APLAYER_PACKAGE_NAME.shortcut.my_love"
    const val ACTION_SHORTCUT_LASTADDED = "$APLAYER_PACKAGE_NAME.shortcut.last_added"
    const val ACTION_CMD = "$APLAYER_PACKAGE_NAME.cmd"
    const val ACTION_WIDGET_UPDATE = "$APLAYER_PACKAGE_NAME.widget_update"
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

    private const val INTERVAL_UPDATE_APPWIDGET = 1000L
    private const val INTERVAL_SAVE_PROGRESS = 1000L
    private const val INTERVAL_CONTROL = 1000

    /**
     * 复制bitmap
     */
    @JvmStatic
    fun copy(bitmap: Bitmap?): Bitmap? {
      if (bitmap == null || bitmap.isRecycled) {
        return null
      }
      val config: Bitmap.Config = bitmap.config ?: return null
      return try {
        bitmap.copy(config, false)
      } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        null
      }
    }
  }
}
