package remix.myplayer.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.service.Command
import remix.myplayer.service.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.service.playback.PlaybackUiState
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Util
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @param:ApplicationContext private val context: Context,
  private val playQueueRepository: PlayQueueRepository,
  private val settingPrefs: SettingPrefs,
) : ViewModel(), MusicEventCallback {

  val playbackUiState: StateFlow<PlaybackUiState> =
    MusicStateSource.playbackUiState
      .debounce(200)
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Eagerly, MusicStateSource.playbackUiState.value)

  // 播放队列
  private val _playQueueSongs = MutableStateFlow<List<Song>>(emptyList())
  val playQueueSongs: StateFlow<List<Song>> = _playQueueSongs.asStateFlow()

  init {
    viewModelScope.launch {
      playQueueRepository.getAllSongs().collect {
        _playQueueSongs.value = it
      }
    }
  }

  fun setProgress(progress: Long) {
    Util.sendLocalBroadcast(
      Intent(MusicService.ACTION_CMD)
        .putExtra(MusicService.EXTRA_COMMAND, Command.SEEK_TO)
        .putExtra(MusicService.EXTRA_PROGRESS, progress)
    )
  }

  private val _seekBarUiState = MutableStateFlow(SeekBarUiState())
  val seekBarUiState = _seekBarUiState.asStateFlow()

  fun setSeekbarUiState(overrideProgress: Long?, interacting: Boolean) {
    _seekBarUiState.value = SeekBarUiState(overrideProgress, interacting)
  }

  fun removeFromQueue(id: Long) {
    MusicServiceRemote.removeFromQueue(listOf(id))
  }

  fun insertToQueue(queue: List<Song>) = viewModelScope.launch {
    MusicServiceRemote.insertToQueue(queue)
    MessageNotifier.show(R.string.add_song_playqueue_success, queue.size)
  }

  private var continuousSeekJob: Job? = null

  fun startContinuousSeek(forward: Boolean) {
    stopContinuousSeek()
    val initialPosition = MusicStateSource.currentProgressState.position
    val duration = MusicStateSource.currentProgressState.duration

    continuousSeekJob = viewModelScope.launch {
      var currentTarget = initialPosition.coerceAtLeast(0L)
      var counter = 0
      while (isActive) {
        delay(CONTINUOUS_SEEK_INTERVAL_MS)

        val multiplier = (counter / CONTINUOUS_SEEK_ACCELERATION_TICKS) + 1L
        val delta = CONTINUOUS_SEEK_STEP_MS * multiplier
        val maxPosition = if (duration > 0L) duration else Long.MAX_VALUE
        val target = if (forward) {
          currentTarget + delta
        } else {
          currentTarget - delta
        }.coerceAtLeast(0L).coerceAtMost(maxPosition)

        setProgress(target)
        setSeekbarUiState(target, true)
        currentTarget = target

        if (target == 0L || (duration > 0L && target == duration)) {
          stopContinuousSeek()
          break
        }
        counter += 1
      }
    }
  }

  fun stopContinuousSeek() {
    if (continuousSeekJob != null) {
      setSeekbarUiState(null, false)
      continuousSeekJob?.cancel()
      continuousSeekJob = null
    }
  }

  private val _swatch = MutableStateFlow(defaultSwatch)
  val swatch = _swatch.asStateFlow()

  fun updateSwatch(bitmap: Bitmap?) {
    if (settingPrefs.playingScreenBackground != SettingPrefs.BACKGROUND_ADAPTIVE_COLOR) {
      return
    }
    if (bitmap == null) {
      _swatch.value = defaultSwatch
      return
    }

    viewModelScope.launch(Dispatchers.Default) {
      val newSwatch: Swatch
      val palette = Palette.from(bitmap).generate()
      if (palette.mutedSwatch != null) {
        newSwatch = palette.mutedSwatch!!
      } else {
        val swatches = ArrayList<Swatch>(palette.swatches)
        swatches.sortWith(Comparator { o1, o2 -> o1.population.compareTo(o2.population) })
        newSwatch = if (swatches.isNotEmpty()) swatches[0] else defaultSwatch
      }
      _swatch.value = newSwatch
    }
  }

  override fun onServiceConnected(service: MusicService) {
    Timber.v("onServiceConnected")
  }

  override fun onServiceDisConnected() {
    Timber.v("onServiceDisConnected")
  }

  override fun onMediaStoreChanged() {
    Timber.v("onMediaStoreChanged")
  }

  override fun onPermissionChanged(has: Boolean) {
    Timber.v("onPermissionChanged: $has")
  }

  override fun onPlayListChanged(name: String) {
  }

  override fun onTagChanged(oldSong: Song?, newSong: Song) {
  }

  companion object {
    private const val CONTINUOUS_SEEK_STEP_MS = 5_000L
    private const val CONTINUOUS_SEEK_INTERVAL_MS = 500L
    private const val CONTINUOUS_SEEK_ACCELERATION_TICKS = 2

    val defaultSwatch = Swatch(Color.GRAY, 100)
  }
}

data class SeekBarUiState(
  val uiProgress: Long? = null,
  val interacting: Boolean = false,
)
