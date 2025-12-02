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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.service.playback.Playback.PlayerCallback
import remix.myplayer.util.Constants.MB
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

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

  override var isPrepared: Boolean = false
    private set

  override var hasError: Boolean = false
    private set

  override val audioSessionId: Int
    get() = player.audioSessionId

  private var callback: PlayerCallback? = null

  private val cache by lazy {
    val base = context.externalCacheDir ?: context.cacheDir
    val dir = File(base, "media3-cache")
    // 后续考虑做成配置
    val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
      sm?.getAllocatableBytes(StorageManager.UUID_DEFAULT) ?: dir.usableSpace
    } else {
      dir.usableSpace
    }
    val size = (availableBytes / 10).coerceIn(128L * MB, 1024L * MB)
    SimpleCache(
      dir,
      LeastRecentlyUsedCacheEvictor(size),
      StandaloneDatabaseProvider(context),
      null,
      false,
      false
    )
  }

  private val scope = CoroutineScope(Dispatchers.Main)
  private var progressTickerJob: Job? = null

  private val player: ExoPlayer =
    ExoPlayer.Builder(context).setUseLazyPreparation(false).build().apply {
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
//              Timber.tag(TAG).v("STATE_READY")
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

  private fun buildSource(song: Song): MediaSource {
    val mediaItem =
      MediaItem.Builder().setUri(song.contentUri).setMediaId(song.id.toString()).build()
    return if (song is Song.Remote) {
      val httpFactory = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(song.headers)
      // 缓存
      val cacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
      ProgressiveMediaSource.Factory(cacheFactory)
        .createMediaSource(mediaItem)
    } else {
      val dataSourceFactory = DefaultDataSource.Factory(context)
      ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(mediaItem)
    }
  }

  override suspend fun prepare(song: Song, nextSong: Song, offset: Long) {
    hasError = false
    isPrepared = false

    val sources = mutableListOf<MediaSource>()
    sources.add(buildSource(song))
    if (nextSong.valid()) {
      sources.add(buildSource(nextSong))
    }

    player.setMediaSources(sources, 0, offset)

    val prepared = try {
      withTimeout(10_000L) { prepareInternal() }
    } catch (e: TimeoutCancellationException) {
      Timber.tag(TAG).w(e)
      false
    }
    if (prepared) {
      isPrepared = true
      callback?.onPrepare()
    } else {
      isPrepared = false
    }
  }

  override fun replaceNext(next: Song) {
    if (!next.valid()) {
      Timber.tag(TAG).w("ignore replaceNext")
      return
    }
    if (player.mediaItemCount == 0) {
      throw IllegalArgumentException("use prepare first")
    }

    val nextIndex = player.currentMediaItemIndex + 1
    if (nextIndex < player.mediaItemCount) {
      player.removeMediaItems(nextIndex, player.mediaItemCount)
    }
    player.addMediaSource(buildSource(next))

    trim()
  }

  override fun appendNext(next: Song) {
    if (!next.valid()) {
      Timber.tag(TAG).w("ignore appendNext")
      return
    }
    player.addMediaSource(buildSource(next))
    trim()
  }

  // 清除已经播放过的mediaItem
  private fun trim() {
    val idx = player.currentMediaItemIndex
    if (idx > 1) {
      player.removeMediaItems(0, idx - 1)
    }
  }

  private suspend fun prepareInternal() = suspendCancellableCoroutine { cont ->
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        Timber.tag(TAG)
          .v("prepareInternal onPlaybackStateChanged, state: $playbackState isActive: ${cont.isActive}")
        if (playbackState == Player.STATE_READY && cont.isActive) {
          cont.resume(true)
          player.removeListener(this)
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        Timber.tag(TAG).v("onPlayerError")
        player.removeListener(this)
        cont.resume(false)
      }
    }

    cont.invokeOnCancellation {
      player.removeListener(listener)
    }

    player.addListener(listener)
    player.prepare()
  }

  override fun start(crossFade: Boolean) {
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
    // TODO release?
    scope.launch(Dispatchers.IO) {
      cache.release()
    }
  }

  override fun duration(): Long {
    val d = player.duration
    return if (d == C.TIME_UNSET) 0 else d
  }

  override fun position(): Long {
    val p = player.currentPosition
    return p.coerceIn(0, duration())
  }

  override fun bufferedPosition(): Long {
    val b = player.bufferedPosition
    return b.coerceIn(0, duration())
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