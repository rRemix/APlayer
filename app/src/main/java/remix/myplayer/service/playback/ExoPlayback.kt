package remix.myplayer.service.playback

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_LOOP
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_REPEAT
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_SHUFFLE
import remix.myplayer.misc.checkMainThread
import remix.myplayer.service.playback.Playback.PlayerCallback
import remix.myplayer.util.Constants.MB
import timber.log.Timber
import java.io.File

@OptIn(UnstableApi::class)
class ExoPlayback(private val context: Context) : Playback {

  override var speed: Float
    get() = player.playbackParameters.speed
    set(value) {
      val current = player.playbackParameters
      player.playbackParameters = PlaybackParameters(value, current.pitch)
    }

  override val isPlaying: Boolean
    get() = player.isPlaying

  override val currentSong: Song?
    get() = player.currentMediaItem?.localConfiguration?.tag as? Song

  override val currentIndex: Int
    get() = player.currentMediaItemIndex

  override val mediaItemCount: Int
    get() = player.mediaItemCount

  override val nextSong: Song?
    get() {
      val index = getNextSongIndex()
      if (index == C.INDEX_UNSET) return null
      val timeline = player.currentTimeline
      val window = androidx.media3.common.Timeline.Window()
      timeline.getWindow(index, window)
      return window.mediaItem.localConfiguration?.tag as? Song
    }

  override var isPrepared: Boolean = false
    private set

  override var hasError: Boolean = false
    private set

  override val audioSessionId: Int
    get() = player.audioSessionId

  private var callback: PlayerCallback? = null

  private val scope = CoroutineScope(Dispatchers.Main)
  private var progressTickerJob: Job? = null

  private val localDataSourceFactory = DefaultDataSource.Factory(context)

  private val player: ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
      playWhenReady = false
      val attrs = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
      setAudioAttributes(attrs, /* handleAudioFocus= */ false)
      setWakeMode(C.WAKE_MODE_LOCAL)
      addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          callback?.onIsPlayingChanged(isPlaying)
          if (isPlaying) startProgressTicker() else stopProgressTicker()
        }

        override fun onPlaybackStateChanged(state: Int) {
          Timber.tag(TAG).v("onPlaybackStateChanged: $state")
          when (state) {
            Player.STATE_READY -> {
              if (!isPrepared) {
                isPrepared = true
                callback?.onPrepare()
              }
            }

            Player.STATE_BUFFERING -> {
              callback?.onPositionChange()
            }

            Player.STATE_ENDED -> {
              callback?.onEnded()
            }

            Player.STATE_IDLE -> {
              isPrepared = false
              stopProgressTicker()
            }
          }
        }

        override fun onPlayerError(error: PlaybackException) {
          hasError = true
          callback?.onError(error)
        }

        override fun onPositionDiscontinuity(
          oldPosition: Player.PositionInfo,
          newPosition: Player.PositionInfo,
          reason: Int
        ) {
          if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            callback?.onPositionChange()
          }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          callback?.onItemTransition(mediaItem, reason)
        }
      })
    }

  private fun startProgressTicker() {
    if (progressTickerJob != null) return
    progressTickerJob = scope.launch {
      while (isActive) {
        callback?.onPositionChange()
        delay(100)
      }
    }
  }

  private fun stopProgressTicker() {
    progressTickerJob?.cancel()
    progressTickerJob = null
  }

  // 构建MediaSource
  private fun buildSource(song: Song): MediaSource {
    val mediaItem =
      MediaItem.Builder().setUri(song.contentUri).setMediaId(song.id.toString()).setTag(song)
        .build()
    return if (song is Song.Remote) {
      val httpFactory = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(song.headers)
      // 缓存
      val cacheFactory = CacheDataSource.Factory()
        .setCache(MediaCache.get(context))
        .setUpstreamDataSourceFactory(httpFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
      ProgressiveMediaSource.Factory(cacheFactory)
        .createMediaSource(mediaItem)
    } else {
      ProgressiveMediaSource.Factory(localDataSourceFactory)
        .createMediaSource(mediaItem)
    }
  }

  override fun setPlaylist(songs: List<Song>, index: Int, offset: Long) {
    checkMainThread()
    hasError = false
    isPrepared = false

    val sources = songs.map { buildSource(it) }
    player.setMediaSources(sources, index, offset)
    player.prepare()
  }

  override fun addSongs(songs: List<Song>, index: Int) {
    checkMainThread()
    val sources = songs.map { buildSource(it) }
    if (index == -1) {
      player.addMediaSources(sources)
    } else {
      val safeIndex = index.coerceIn(0, player.mediaItemCount)
      player.addMediaSources(safeIndex, sources)
    }
  }

  override fun removeSong(index: Int) {
    checkMainThread()
    if (index in 0 until player.mediaItemCount) {
      player.removeMediaItem(index)
    }
  }

  override fun setMode(mode: Int) {
    checkMainThread()
    when (mode) {
      MODE_LOOP -> {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled = false
      }

      MODE_REPEAT -> {
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.shuffleModeEnabled = false
      }

      MODE_SHUFFLE -> {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled = true
      }

      else -> {
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false
      }
    }
  }

  override fun skipToNext() {
    checkMainThread()
    val timeline = player.currentTimeline
    if (timeline.isEmpty) return

    // 无论什么模式，手动切歌都应该跳到下一首（在单曲循环下需要强制切到下一首）
    val nextIndex = timeline.getNextWindowIndex(
      player.currentMediaItemIndex,
      Player.REPEAT_MODE_ALL,
      player.shuffleModeEnabled
    )

    if (nextIndex != C.INDEX_UNSET) {
      player.seekToDefaultPosition(nextIndex)
    }
  }

  override fun skipToPrevious() {
    checkMainThread()
    val timeline = player.currentTimeline
    if (timeline.isEmpty) return

    // 类似 skipToNext，单曲循环下也需要切到上一首
    val previousIndex = timeline.getPreviousWindowIndex(
      player.currentMediaItemIndex,
      Player.REPEAT_MODE_ALL,
      player.shuffleModeEnabled
    )
    if (previousIndex != C.INDEX_UNSET) {
      player.seekToDefaultPosition(previousIndex)
    }
  }

  override fun skipTo(index: Int) {
    checkMainThread()
    player.seekTo(index, C.TIME_UNSET)
  }

  // 根据播放模式获取下一首索引
  private fun getNextSongIndex(): Int {
    val timeline = player.currentTimeline
    if (timeline.isEmpty) return C.INDEX_UNSET

    return timeline.getNextWindowIndex(
      player.currentMediaItemIndex,
      player.repeatMode,
      player.shuffleModeEnabled
    )
  }

  // 从Timeline获取当前播放列表
  override fun getPlaylist(): List<Song> {
    val timeline = player.currentTimeline
    val list = ArrayList<Song>()
    val window = Timeline.Window()
    for (i in 0 until timeline.windowCount) {
      timeline.getWindow(i, window)
      val mediaItem = window.mediaItem
      (mediaItem.localConfiguration?.tag as? Song)?.let {
        list.add(it)
      }
    }
    return list
  }

  override fun start() {
    player.play()
  }

  override fun pause() {
    player.pause()
  }

  override fun release() {
    stopProgressTicker()
    player.release()
    isPrepared = false
    callback = null
    scope.cancel()
  }

  override val duration: Long
    get() {
      val d = player.duration
      return if (d == C.TIME_UNSET) 0 else d
    }

  override val position: Long
    get() {
      val p = player.currentPosition
      return p.coerceIn(0, duration)
    }

  override val bufferedPosition: Long
    get() {
      val b = player.bufferedPosition
      return b.coerceIn(0, duration)
    }

  override fun seek(pos: Long) {
    player.seekTo(pos)
  }

  override fun setVolume(volume: Float) {
    player.volume = volume
  }

  fun attach(cb: PlayerCallback) {
    this.callback = cb
  }

  companion object {
    private const val TAG = "ExoPlayback"
  }
}

@OptIn(UnstableApi::class)
private object MediaCache {
  @Volatile
  private var cache: SimpleCache? = null

  fun get(context: Context): SimpleCache {
    return cache ?: synchronized(this) {
      cache ?: buildCache(context).also { cache = it }
    }
  }

  private fun buildCache(context: Context): SimpleCache {
    val appContext = context.applicationContext
    val base = appContext.externalCacheDir ?: appContext.cacheDir
    val dir = File(base, "media3-cache")
    val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val sm = appContext.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
      try {
        sm?.getAllocatableBytes(StorageManager.UUID_DEFAULT) ?: dir.usableSpace
      } catch (e: Exception) {
        dir.usableSpace
      }
    } else {
      dir.usableSpace
    }
    val size = (availableBytes / 10).coerceIn(128L * MB, 1024L * MB)
    Timber.tag("ExoPlayback").v("cacheSize: $size")
    return SimpleCache(
      dir,
      LeastRecentlyUsedCacheEvictor(size),
      StandaloneDatabaseProvider(appContext),
      null,
      false,
      false
    )
  }
}
