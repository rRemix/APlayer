package remix.myplayer.compose.viewmodel

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.repo.PlayQueueRepository
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.Constants.MODE_LOOP
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val playQueueRepository: PlayQueueRepository
) : ViewModel(), MusicEventCallback {

  init {
    viewModelScope.launch {
      playQueueRepository.getAllSongs().collect {
        _playQueueSongs.value = it
      }
    }
  }

  private var serviceRef: WeakReference<MusicService>? = null

  val lastOp: Int
    get() = serviceRef?.get()?.operation ?: Command.NEXT

  private val _musicState = MutableStateFlow(MusicState())
  val musicState = _musicState.asStateFlow()

  private val _playQueueSongs = MutableStateFlow<List<Song>>(emptyList())
  val playQueueSongs: StateFlow<List<Song>> = _playQueueSongs.asStateFlow()

  fun updateMusicState(
    song: Song = _musicState.value.song,
    newSong: Song = serviceRef?.get()?.nextSong ?: Song.EMPTY_SONG,
    playing: Boolean = _musicState.value.playing,
    playModel: Int = serviceRef?.get()?.playModel ?: MODE_LOOP
  ) {
    _musicState.value = _musicState.value.copy(
      song = song,
      nextSong = newSong,
      playing = playing,
      playMode = playModel
    )
  }

  fun setPlayModel(mode: Int) {
    serviceRef?.get()?.playModel = mode
    updateMusicState()
  }

  fun setProgress(progress: Int) {
    serviceRef?.get()?.setProgress(progress)
  }

  fun getProgress(): Int {
    return serviceRef?.get()?.progress ?: 0
  }

  fun removeFromQueue(id: Long) {
    viewModelScope.launch {
      val count = playQueueRepository.remove(listOf(id))
      if (count > 0 && id == _musicState.value.song.id) {
        Util.sendCMDLocalBroadcast(Command.NEXT)
      }
    }
  }

  fun insertToQueue(queue: List<Long>) {
    viewModelScope.launch {
      val ids = playQueueRepository.insert(queue)
      ToastUtil.show(
        context,
        context.getString(R.string.add_song_playqueue_success, ids.filter { it > 0 }.size)
      )
    }
  }

  override fun onMediaStoreChanged() {
    Timber.v("onMediaStoreChanged")
  }

  override fun onPermissionChanged(has: Boolean) {
    Timber.v("onPermissionChanged: $has")
  }

  override fun onPlayListChanged(name: String) {
  }

  private var updateProgressJob: Job? = null
  override fun onServiceConnected(service: MusicService) {
    Timber.v("onServiceConnected")

    serviceRef = WeakReference(service)

//    updateProgressJob?.cancel()
//    updateProgressJob = viewModelScope.launch {
//      while (true) {
//        delay(500)
//        val service = serviceRef?.get() ?: return@launch
//        if (!service.isPlaying) {
//          continue
//        }
//        _progressState.value = service.progress
//      }
//    }

    onMetaChanged()
  }

  override fun onMetaChanged() {
    Timber.v("onMetaChanged")

    val service = serviceRef?.get() ?: return
    updateMusicState(
      song = service.currentSong,
      playing = service.isPlaying
    )
  }

  override fun onPlayStateChange() {
    Timber.v("onPlayStateChange")

    val service = serviceRef?.get() ?: return
    updateMusicState(playing = service.isPlaying)
  }

  override fun onServiceDisConnected() {
    Timber.v("onServiceDisConnected")
    serviceRef?.clear()
    serviceRef = null
  }

  override fun onTagChanged(
    oldSong: Song,
    newSong: Song
  ) {
  }
}

@Stable
data class MusicState(
  val song: Song = Song.EMPTY_SONG,
  val nextSong: Song = Song.EMPTY_SONG,
  val playing: Boolean = false,
  val playMode: Int = MODE_LOOP
)