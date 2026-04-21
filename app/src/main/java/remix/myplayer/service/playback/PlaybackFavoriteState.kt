package remix.myplayer.service.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.repo.PlayListRepository
import javax.inject.Inject

class PlaybackFavoriteState @Inject constructor(
  private val playListRepository: PlayListRepository
) {

  private var lookupJob: Job? = null

  fun refresh(scope: CoroutineScope, song: Song, stateSource: StateSource) {
    cancelLookup()
    if (!song.isLocal() || !song.valid()) {
      return
    }

    val songId = song.id
    lookupJob = scope.launch {
      val favorite = withContext(Dispatchers.IO) {
        playListRepository.isFavorite(songId)
      }
      if (stateSource.currentPlaybackUiState.song.id == songId) {
        stateSource.updatePlaybackUiState(isFavorite = favorite)
      }
    }
  }

  suspend fun toggle(song: Song, stateSource: StateSource): Boolean {
    cancelLookup()
    if (!song.isLocal() || !song.valid()) {
      return false
    }

    val songId = song.id
    val favorite = withContext(Dispatchers.IO) {
      playListRepository.toggleFavorite(songId)
    }
    if (stateSource.currentPlaybackUiState.song.id != songId) {
      return false
    }
    stateSource.updatePlaybackUiState(isFavorite = favorite)
    return true
  }

  fun cancelLookup() {
    lookupJob?.cancel()
    lookupJob = null
  }
}
