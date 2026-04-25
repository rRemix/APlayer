package remix.myplayer.service.playback

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.service.Command

interface StateSource {

  val playbackUiState: StateFlow<PlaybackUiState>
  val progressState: StateFlow<ProgressUiState>

  val currentPlaybackUiState: PlaybackUiState
  val currentProgressState: ProgressUiState

  fun updatePlaybackUiState(
    song: Song? = null,
    nextSong: Song? = null,
    isPlaying: Boolean? = null,
    isFavorite: Boolean? = null,
    speed: Float? = null,
    playModel: Int? = null,
    lastOp: Int? = null,
  )

  fun updateProgressUiState(position: Long? = null, duration: Long? = null, buffered: Long? = null)
}

object MusicStateSource : StateSource {

  // 基础的播放状态
  private val _playBackUiState = MutableStateFlow(PlaybackUiState())
  override val playbackUiState = _playBackUiState

  // 单独的播放进度状态
  private val _progressState = MutableStateFlow(ProgressUiState())
  override val progressState: StateFlow<ProgressUiState> = _progressState

  override val currentPlaybackUiState: PlaybackUiState
    get() = _playBackUiState.value
  override val currentProgressState: ProgressUiState
    get() = _progressState.value

  override fun updatePlaybackUiState(
    song: Song?,
    nextSong: Song?,
    isPlaying: Boolean?,
    isFavorite: Boolean?,
    speed: Float?,
    playModel: Int?,
    lastOp: Int?,
  ) {
    _playBackUiState.update { old ->
      val newSong = song ?: old.song
      val songChanged = song != null && newSong.id != old.song.id
      old.copy(
        song = newSong,
        nextSong = nextSong ?: old.nextSong,
        isPlaying = isPlaying ?: old.isPlaying,
        isFavorite = isFavorite ?: if (songChanged) false else old.isFavorite,
        speed = speed ?: old.speed,
        playMode = playModel ?: old.playMode,
        lastOp = lastOp ?: old.lastOp,
        seq = old.seq + 1
      )
    }
  }

  override fun updateProgressUiState(
    position: Long?,
    duration: Long?,
    buffered: Long?
  ) {
    _progressState.update {
      it.copy(
        position = position ?: it.position,
        duration = duration ?: it.duration,
        buffered = buffered ?: it.buffered
      )
    }
  }

}

@Immutable
data class PlaybackUiState(
  val song: Song = Song.EMPTY_SONG,
  val nextSong: Song = Song.EMPTY_SONG,
  val isPlaying: Boolean = false,
  val isFavorite: Boolean = false,
  val speed: Float = 1.0f,
  val playMode: Int = SettingPrefs.MODE_LOOP,
  val lastOp: Int = Command.SKIP_TO_NEXT,
  val seq: Int = 0
)

@Immutable
data class ProgressUiState(
  val position: Long = 0L,
  val duration: Long = 0L,
  val buffered: Long = 0L,
)
