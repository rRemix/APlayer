package remix.myplayer.service.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import remix.myplayer.data.bean.mp3.Song

interface Playback {

  var speed: Float

  val isPlaying: Boolean

  val isPrepared: Boolean

  val audioSessionId: Int

  suspend fun prepare(song: Song, nextSong: Song, offset: Long = 0)

  fun replaceNext(next: Song)

  fun appendNext(next: Song)

  fun start(crossFade: Boolean = false)

  fun pause()

  fun release()

  fun duration(): Long

  fun position(): Long

  fun bufferedPosition(): Long

  fun seek(pos: Long)

  fun setVolume(volume: Float)

  interface PlayerCallback {

    fun onIsPlayingChanged(isPlaying: Boolean)
    fun onPrepare()
    fun onEnded()
    fun onError(error: PlaybackException)
    fun onItemTransition(mediaItem: MediaItem?, reason: Int)
    fun onPositionChange()
  }
}