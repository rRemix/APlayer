package remix.myplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.helper.SortOrder
import remix.myplayer.repo.SongRepository
import timber.log.Timber

data class DetailState(
  val songs: List<Song> = emptyList(),
  val albums: List<Album> = emptyList(),
  val coverSongs: List<Song> = emptyList(),
  val duration: Long = 0,
  val loading: Boolean = true,
  val failed: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
  private val songRepository: SongRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(DetailState())
  val state = _state.asStateFlow()
  private var loadJob: Job? = null

  fun refresh(model: APlayerModel) {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      // Keep the existing content while refreshing so returning from an album preserves scrolling.
      _state.value = _state.value.copy(loading = true, failed = false)
      try {
        songRepository.observeSongs(model).collect { songs ->
          _state.value = withContext(Dispatchers.Default) { detailState(model, songs) }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.w(e, "Unable to load details")
        _state.value = _state.value.copy(loading = false, failed = true)
      }
    }
  }
}

/** Albums containing this artist's local songs, including compilations. Counts are artist-scoped. */
internal fun artistAlbums(songs: List<Song>): List<Album> {
  val albums = songs.groupBy { it.albumId }.map { (albumId, tracks) ->
    val song = tracks.first()
    Album(albumId, song.album, song.artistId, song.artist, tracks.size)
  }
  return ItemsSorter.sortedAlbums(albums, SortOrder.ALBUM_A_Z)
}

/** Build UI values from an immutable song snapshot. */
internal fun detailState(model: APlayerModel, songs: List<Song>) = DetailState(
  songs = songs,
  albums = if (model is Artist) artistAlbums(songs) else emptyList(),
  coverSongs = detailCoverSongs(songs),
  duration = songs.sumOf { it.duration.coerceAtLeast(0L) },
  loading = false,
)

/** Keep every song as a fallback, with an order independent of the song list's sort setting. */
internal fun detailCoverSongs(songs: List<Song>): List<Song> = songs.sortedWith(
  compareBy<Song> { it.albumId }.thenBy { it.getKey() }.thenBy { it.data },
)
