package remix.myplayer.service.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import remix.myplayer.data.model.audio.Song

interface Playback {

  /** 播放速度 */
  var speed: Float

  /** 是否正在播放 */
  val isPlaying: Boolean

  /** 当前播放的歌曲 */
  val currentSong: Song?

  /** 当前播放索引 */
  val currentIndex: Int

  /** 播放列表大小 */
  val itemCount: Int

  /** 下一首歌曲 */
  val nextSong: Song?

  /** 是否准备完成 */
  val isPrepared: Boolean

  /** AudioSessionId */
  val audioSessionId: Int

  /** 设置播放列表 */
  fun setPlaylist(songs: List<Song>, index: Int = 0, offset: Long = 0)

  /** 添加歌曲 (index = -1 表示添加到末尾) */
  fun addSongs(songs: List<Song>, index: Int = -1)

  /** 添加到下一首播放*/
  fun addToNextSong(nextSong: Song): Boolean

  /** 移除指定位置的歌曲 */
  fun removeSong(index: Int)

  /** 替换歌曲 */
  fun replaceSong(song: Song)

  /** 设置播放模式 */
  fun setMode(mode: Int, listLoop: Boolean)

  /** 切换下一首 */
  fun skipToNext()

  /** 切换上一首 */
  fun skipToPrevious()

  /** 跳至指定位置 */
  fun skipTo(index: Int)

  /** 获取当前播放列表 */
  fun getPlaylist(): List<Song>

  /** 开始播放 */
  fun start()

  /** 暂停 */
  fun pause()

  /** 释放资源 */
  fun release()

  /** 歌曲总时长 */
  val duration: Long

  /** 当前播放进度 */
  val position: Long

  /** 缓冲进度 */
  val bufferedPosition: Long

  /** 拖动进度 */
  fun seek(pos: Long)

  /** 设置音量 */
  fun setVolume(volume: Float)

  interface PlayerCallback {

    fun onIsPlayingChanged(isPlaying: Boolean)
    fun onAudioSessionIdChanged(audioSessionId: Int)
    fun onPrepare()
    fun onEnded()
    fun onError(error: PlaybackException)
    fun onItemTransition(mediaItem: MediaItem?, reason: Int)
    fun onPositionChange()
  }
}
