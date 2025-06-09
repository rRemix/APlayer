package remix.myplayer.compose.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import remix.myplayer.bean.mp3.Song
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) :
  ViewModel() {

  private val _currentSong = MutableStateFlow<Song>(Song.EMPTY_SONG)
  val currentSong = _currentSong

  fun setCurrentSong(song: Song) {
    _currentSong.value = song
  }

  private val _playing = MutableStateFlow(false)
  val playing = _playing

  fun setPlaying(playing: Boolean) {
    _playing.value = playing
  }
}