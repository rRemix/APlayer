package remix.myplayer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.LanguageHelper
import remix.myplayer.helper.ShakeDetector
import remix.myplayer.helper.SleepTimer
import remix.myplayer.lyric.LyricManager
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.misc.receiver.MediaButtonReceiver
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
import remix.myplayer.service.playback.PlaybackFavoriteState
import remix.myplayer.service.playback.PlaybackProgressSaver
import remix.myplayer.ui.activity.LockScreenActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PERMISSION
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Constants.ACTION_EXIT
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import remix.myplayer.util.ext.checkMainThread
import remix.myplayer.util.ext.getPendingIntentFlag
import remix.myplayer.util.ext.tryLaunch
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
  lateinit var appWidgetUpdater: AppWidgetUpdater

  @Inject
  lateinit var mediaSessionUpdater: MediaSessionUpdater

  @Inject
  lateinit var lyricManager: LyricManager

  @Inject
  lateinit var settingPrefs: SettingPrefs

  @Inject
  lateinit var playbackProgressSaver: PlaybackProgressSaver

  @Inject
  lateinit var playbackFavoriteState: PlaybackFavoriteState

  @Inject
  lateinit var playQueueStore: PlayQueueStore

  @Inject
  lateinit var playListRepository: PlayListRepository

  @Inject
  lateinit var songRepository: SongRepository

  @Inject
  lateinit var historyRepository: HistoryRepository

  @Inject
  lateinit var fetchMetaDataUseCase: FetchMetaDataUseCase

  private val stateSource = MusicStateSource

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
  private var playModel: Int
    get() = settingPrefs.playModel
    set(value) {
      Timber.v("修改播放模式: $value")
      settingPrefs.playModel = value
      playback.setMode(value, settingPrefs.listLoop)
      updateMediaSessionQueue()
      pushPlaybackUiState()
      appWidgetUpdater.partiallyUpdateWidget(this)
    }

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
   * MediaSession
   */
  lateinit var mediaSession: MediaSessionCompat
    private set

  /**
   * 通知栏
   */
  private lateinit var notify: Notify

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
   * 准备歌曲
   */
  private var prepareJob: Job? = null

  /**
   * 操作类型
   */
  private var lastCommand: Int = -1
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
    get() = playback.isPlaying

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
    val command = commandIntent?.getIntExtra(EXTRA_COMMAND, -1)
    val action = commandIntent?.action

    Timber.v("onStartCommand, action: $action command: $command flags: $flags startId: $startId")
    stop = false

    tryLaunch {
      if (action == ACTION_CMD && command == Command.CLOSE_NOTIFY) {
        handleStartCommandIntent(commandIntent, action)
        return@tryLaunch
      }

      hasPermission = PermissionUtil.hasNecessaryPermission()
      load()
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
          notify.stopForegroundAndNotification()
          updateNotification()
        }
      }
      // 锁屏
      PrefKeys.Setting.LOCKSCREEN -> {
        when (settingPrefs.lockScreen) {
          LOCKSCREEN_CLOSE -> mediaSessionUpdater.clear(mediaSession)
          LOCKSCREEN_SYSTEM, LOCKSCREEN_APLAYER -> mediaSessionUpdater.updateMetadata(
            this,
            mediaSession,
            playback,
            settingPrefs.lockScreen,
            lyricManager.isDesktopLyricLocked
          )
        }
      }
      // 断点播放
      PrefKeys.Setting.PLAY_AT_BREAKPOINT -> {
        if (!settingPrefs.playAtBreakPoint) {
          playbackProgressSaver.stop()
        } else {
          playbackProgressSaver.start(this) { playback.position }
        }
      }
      // 倍速播放
      PrefKeys.Setting.SPEED -> {
        playback.speed = settingPrefs.speedValue
        pushPlaybackUiState()
      }
      // 列表循环
      PrefKeys.Setting.LIST_LOOP -> {
        playback.setMode(playModel, settingPrefs.listLoop)
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(headSetReceiver, noisyFilter, RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(headSetReceiver, noisyFilter)
    }

    registerLocalReceiver(appWidgetUpdater.receiver, IntentFilter(ACTION_WIDGET_UPDATE))
    val screenFilter = IntentFilter()
    screenFilter.addAction(Intent.ACTION_SCREEN_ON)
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(screenReceiver, screenFilter)
    }

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
        Timber.v("onMediaButtonEvent")
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
        notify.stopForegroundAndNotification()
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
    playback = ExoPlayback(this, settingPrefs.decoderMode, audioFocusManager)
    playback.attach(this)

    Timber.v("setUpPlayback, audioSessionId: ${playback.audioSessionId}")
    EQHelper.updateAudioSession(this, playback.audioSessionId)
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    Timber.v("onIsPlayingChanged: $isPlaying")
    stateSource.updatePlaybackUiState(isPlaying = isPlaying)
    if (isPlaying) {
      updatePlayHistory()
    }
  }

  override fun onAudioSessionIdChanged(audioSessionId: Int) {
    Timber.v("onAudioSessionIdChanged, audioSessionId: ${playback.audioSessionId}")
    EQHelper.updateAudioSession(this, audioSessionId)
  }

  override fun onPrepare() {
    Timber.v("onPrepare, firstPrepared: $firstPrepared")
    EQHelper.updateAudioSession(this, playback.audioSessionId)

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
    if (playback.itemCount == 0) {
      notify.stopForegroundAndNotification()
    }
  }

  override fun onItemTransition(mediaItem: MediaItem?, reason: Int) {
    Timber.v("onItemTransition, id: ${mediaItem?.mediaId} reason: $reason playing: $isPlaying currentSong: ${playback.currentSong?.title}")

    Timber.v("onItemTransition, playback.audioSessionId: ${playback.audioSessionId}")
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
        lastCommand = Command.PLAY
      } else {
        lastCommand = Command.SKIP_TO_NEXT
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

    playbackProgressSaver.stop()
    playbackFavoriteState.cancelLookup()
    appWidgetUpdater.stop()
    cancel()

    EQHelper.releaseCurrentAudioSession(this)
    if (isPlaying) {
      pause()
    }
    playback.release()
    load = 0

    notify.stopForegroundAndNotification()

    lyricManager.isServiceAvailable = false

    updateNotification()

    audioFocusManager.detach()

    mediaSession.isActive = false
    mediaSession.release()

    unregisterLocalReceiver(controlReceiver)
    unregisterLocalReceiver(musicEventReceiver)
    unregisterLocalReceiver(appWidgetUpdater.receiver)
    Util.unregisterReceiver(this, headSetReceiver)
    Util.unregisterReceiver(this, screenReceiver)

    getSharedPreferences(PrefKeys.Setting.NAME, MODE_PRIVATE)
      .unregisterOnSharedPreferenceChangeListener(this)

    contentResolver.unregisterContentObserver(mediaStoreObserver)

    ShakeDetector.getInstance().stopListen()

    alreadyUnInit = true
  }

  private fun updateMediaSessionQueue() {
    mediaSessionUpdater.updateMediaSessionQueue(this, mediaSession, playback.getPlaylist(), playback.currentSong?.title)
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
    updateMediaSessionQueue()
    launch { playQueueStore.save(newQueue) }
  }

  /**
   * 设置播放队列
   */
  fun setPlayQueue(newQueue: List<Song>?, intent: Intent) {
    Timber.v("setPlayQueue")
    // 如果是随机播放，需要更新播放模式
    val shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false)
    if (newQueue.isNullOrEmpty()) {
      return
    }

    //设置的播放队列相等
    val equals = newQueue == playback.getPlaylist()
    if (!equals) {
      playback.setPlaylist(newQueue)
      launch { playQueueStore.save(newQueue) }
    }
    if (shuffle) {
      playModel = MODE_SHUFFLE
    }
    handleCommand(intent)

    if (equals) {
      return
    }
    updateMediaSessionQueue()
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
        launch { playQueueStore.save(playback.getPlaylist()) }

        updateMediaSessionQueue()
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
      launch { playQueueStore.save(playback.getPlaylist()) }
      pushPlaybackUiState()
    }
  }

  /**
   * 播放下一首
   */
  private fun skipToNext() {
    playback.skipToNext()
    start(true)
  }

  /**
   * 播放上一首
   */
  private fun skipToPrevious() {
    playback.skipToPrevious()
    start(true)
  }

  /**
   * 播放
   */
  private fun start(crossFade: Boolean) {
    Timber.v("play: $crossFade")
//    if (!playback.isPrepared) {
//      MessageNotifier.show(R.string.buffering_wait)
//      return
//    }

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
  private fun pushPlaybackUiState() {
    val currentSong = playback.currentSong ?: EMPTY_SONG
    val songChanged = currentSong.id != stateSource.currentPlaybackUiState.song.id

    MusicStateSource.updatePlaybackUiState(
      song = currentSong,
      nextSong = playback.nextSong ?: EMPTY_SONG,
      isPlaying = playback.isPlaying,
      speed = settingPrefs.speedValue,
      playModel = playModel,
      lastOp = lastCommand
    )
    if (songChanged) {
      playbackFavoriteState.refresh(this, currentSong, stateSource)
    }
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

    if (position == -1 || position >= playback.itemCount) {
      MessageNotifier.show(R.string.illegal_arg)
      return
    }

    playback.skipTo(position)
    start(true)
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
    updateMediaSessionQueue()
    pushPlaybackUiState()
  }

  override fun onPlayListChanged(name: String) {
    Timber.v("onPlayListChanged: $name")
  }

  override fun onServiceConnected(service: MusicService) {

  }

  override fun onServiceDisConnected() {

  }

  inner class MusicEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      handleMusicEvent(intent)
    }
  }

  private fun handleStartCommandIntent(commandIntent: Intent?, action: String?) {
    Timber.v("handleStartCommandIntent: $commandIntent")
    if (action == null) {
      return
    }
    firstPrepared = false
    when (action) {
      ACTION_APPWIDGET_OPERATE -> {
        handleCommand(
          Intent(ACTION_CMD).putExtra(
            EXTRA_COMMAND,
            commandIntent?.getIntExtra(EXTRA_COMMAND, -1)
          )
        )
      }

      ACTION_SHORTCUT_SHUFFLE -> {
        if (playModel != MODE_SHUFFLE) {
          playModel = MODE_SHUFFLE
        }
        handleCommand(Intent(ACTION_CMD).putExtra(EXTRA_COMMAND, Command.SKIP_TO_NEXT))
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
            putExtra(EXTRA_COMMAND, Command.PLAY_AT)
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
          lastedIntent.putExtra(EXTRA_COMMAND, Command.PLAY_AT)
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
    appWidgetUpdater.updateWidget(this, this, isPlaying, screenOn)

    // 正在播放、已有通知在显示、用户操作过
    if (isPlaying || notify.isNotifyShowing || lastCommand != -1) {
      updateNotification()
    }
    mediaSessionUpdater.updateMetadata(
      this,
      mediaSession,
      playback,
      settingPrefs.lockScreen,
      lyricManager.isDesktopLyricLocked
    )
    // 是否需要保存进度
    if (settingPrefs.playAtBreakPoint) {
      playbackProgressSaver.start(this) { playback.position }
    }
    // 保存当前播放歌曲
    settingPrefs.lastSong = if (song.isLocal()) song.id.toString() else song.data
  }

  fun updateNotification() {
    notify.updateAndNotify()
  }

  fun updateNotificationWithLrc(lrc: String) {
    notify.updateWithLyric(lrc)
  }

  fun clearStatusBarLyricNotification() {
    notify.clearStatusBarLyricNotification()
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

  private fun handleCommand(intent: Intent?) = launch {
    if (intent == null || intent.extras == null) {
      return@launch
    }
    val command = intent.getIntExtra(EXTRA_COMMAND, -1)
    Timber.v("handleCommand, command: $command")

    if (shouldThrottleCommand(command)) {
      val now = System.currentTimeMillis()
      if (now - lastCommandTime < INTERVAL_CONTROL) {
        Timber.w("ignore command")
        return@launch
      }
      lastCommandTime = now
    }

    val requiresQueue = command == Command.PLAY_AT
        || command == Command.SKIP_TO_PREVIOUS
        || command == Command.SKIP_TO_NEXT
        || command == Command.PLAY_PAUSE
        || command == Command.PAUSE
        || command == Command.PLAY

    if (requiresQueue && playback.itemCount == 0) {
      load()
      if (playback.itemCount == 0) return@launch
    }

    lastCommand = command
    when (command) {
      // 关闭通知栏
      Command.CLOSE_NOTIFY -> {
        pause()
        delay(300)
        notify.stopForegroundAndNotification()
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
        playback.currentSong?.let {
          if (playbackFavoriteState.toggle(it, stateSource)) {
            appWidgetUpdater.updateWidget(this@MusicService, this, isPlaying, screenOn)
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
          lastCommand = Command.PLAY_TEMP
          val song = it as Song.Local

          if (playback.currentSong?.id == song.id) {
            // 如果是当前歌曲，从头播放
            seekTo(0)
          } else {
            playback.addToNextSong(song)
            skipToNext()
          }

          playQueueStore.save(playback.getPlaylist())
          start(true)
        }
      }
      // 解锁桌面歌词
      Command.UNLOCK_DESKTOP_LYRIC -> {
        lyricManager.isDesktopLyricLocked = false
      }
      // 某一首歌曲添加至下一首播放
      Command.ADD_TO_NEXT_SONG -> {
        val nextSong = intent.getSerializableExtra(EXTRA_SONG) as Song? ?: return@launch

        if (playback.addToNextSong(nextSong)) {
          // 同步更新
          playQueueStore.save(playback.getPlaylist())
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

  private fun shouldThrottleCommand(command: Int): Boolean {
    return command == Command.PLAY_AT
        || command == Command.SKIP_TO_PREVIOUS
        || command == Command.SKIP_TO_NEXT
        || command == Command.PLAY_PAUSE
        || command == Command.PAUSE
        || command == Command.PLAY
  }

  fun updatePlaybackState() {
    mediaSessionUpdater.updatePlaybackState(this, mediaSession, playback, lyricManager.isDesktopLyricLocked)
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
      playQueueStore.restore()
    }

    if (queue.isNotEmpty()) {
      playback.setPlaylist(
        queue,
        pos,
        if (firstPrepared && settingPrefs.lastProgress > 0) settingPrefs.lastProgress.toLong() else 0L
      )
      playback.setMode(playModel, settingPrefs.listLoop)
    }
  }

  override fun onFocusGained() {
    Timber.v("onFocusGained")
    if (wasPlaying) {
      start(true)
      wasPlaying = false
      lastCommand = Command.PLAY_PAUSE
    }
    volumeController.directTo(1f)
  }

  override fun onFocusLost() {
    Timber.v("onFocusLost")
    val ignoreFocus = settingPrefs.ignoreAudioFocus
    if (ignoreFocus && !audioFocusManager.shouldPauseForPhoneCall()) {
      Timber.v("忽略音频焦点 不暂停")
      return
    }
    if (isPlaying) {
      lastCommand = Command.PLAY_PAUSE
      pause()
    }
  }

  private var wasPlaying = false
  override fun onFocusLostTransient() {
    Timber.v("onFocusLostTransient")
    wasPlaying = isPlaying
    if (isPlaying) {
      lastCommand = Command.PLAY_PAUSE
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
        appWidgetUpdater.updateWidget(this@MusicService, this@MusicService, isPlaying, screenOn)
      } else {
        screenOn = false
        //停止更新桌面部件
        appWidgetUpdater.stop()
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

    const val EXTRA_COMMAND = "command"
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

    private const val INTERVAL_CONTROL = 500
  }
}
