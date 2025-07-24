package remix.myplayer.compose.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.repo.AlbumRepository
import remix.myplayer.compose.repo.ArtistRepository
import remix.myplayer.compose.repo.FolderRepository
import remix.myplayer.compose.repo.GenreRepository
import remix.myplayer.compose.repo.PlayListRepository
import remix.myplayer.compose.repo.SongRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.glide.UriFetcher
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.ToastUtil
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  private val albumRepo: AlbumRepository,
  private val artistRepo: ArtistRepository,
  private val genreRepo: GenreRepository,
  private val playListRepository: PlayListRepository,
  private val folderRepository: FolderRepository,
  val settingPrefs: SettingPrefs,
) : ViewModel(), MusicEventCallback {

  private var hasPermission = false

  private val _songs = MutableStateFlow<List<Song>>(emptyList())
  val songs: StateFlow<List<Song>> = _songs.asStateFlow()

  private val _albums = MutableStateFlow<List<Album>>(emptyList())
  val albums: StateFlow<List<Album>> = _albums.asStateFlow()

  private val _artists = MutableStateFlow<List<Artist>>(emptyList())
  val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

  private val _genres = MutableStateFlow<List<Genre>>(emptyList())
  val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

  private val _playLists = MutableStateFlow<List<PlayList>>(emptyList())
  val playLists: StateFlow<List<PlayList>> = _playLists.asStateFlow()

  private val _folders = MutableStateFlow<List<Folder>>(emptyList())
  val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

  init {
    // load all media
    hasPermission = PermissionUtil.hasNecessaryPermission()
    if (hasPermission) {
      fetchMedia()
    }

    viewModelScope.launch {
      playListRepository.allPlayLists().collect {
        _playLists.value = it
      }
    }
  }

  fun fetchSongs() = viewModelScope.launch(Dispatchers.IO) {
    _songs.value = songRepo.allSongs()
  }

  fun fetchAlbums() = viewModelScope.launch(Dispatchers.IO) {
    _albums.value = albumRepo.allAlbums()
  }

  fun fetchArtists() = viewModelScope.launch(Dispatchers.IO) {
    _artists.value = artistRepo.allArtists()
  }

  fun fetchGenres() = viewModelScope.launch(Dispatchers.IO) {
    _genres.value = genreRepo.allGenres()
  }

  fun fetchFolders() = viewModelScope.launch(Dispatchers.IO) {
    _folders.value = folderRepository.allFolders()
  }

  fun insertPlayList(name: String, onSuccess: (Long) -> Unit) {
    viewModelScope.launch {
      val id = playListRepository.insertPlayList(name)
      onSuccess(id)
    }
  }

  fun addSongsToPlayList(audioIds: List<Long>, playListName: String, createNew: Boolean = false) {
    viewModelScope.launch {
      try {
        if (createNew) {
          playListRepository.insertPlayList(playListName)
        }

        val count = playListRepository.addSongsToPlayList(audioIds, playListName = playListName)
        ToastUtil.show(
          context,
          context.getString(R.string.add_song_playlist_success, count, playListName)
        )
      } catch (ignore: Exception) {
        ToastUtil.show(context, R.string.add_song_playlist_error)
      }
    }
  }

  fun loadSongsByModels(models: List<APlayerModel>) = songRepo.getSongsByModels(models)

  fun updatePlayList(playList: PlayList) {
    viewModelScope.launch {
      try {
        playListRepository.updatePlayList(playList)
        ToastUtil.show(context, R.string.save_success)
      } catch (e: Exception) {
        ToastUtil.show(context, R.string.save_error)
      }
    }
  }

  fun fetchMedia(clear: Boolean = false) {
    if (clear) {
      UriFetcher.updateAllVersion()
      UriFetcher.clearAllCache()
      Glide.get(context).clearMemory()
    }

    // TODO compare list
    fetchSongs()
    fetchAlbums()
    fetchArtists()
    fetchGenres()
    fetchFolders()
  }

  override fun onMediaStoreChanged() {
    if (hasPermission) {
      fetchMedia()
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has && !hasPermission) {
      fetchMedia()
    }
    hasPermission = has
  }

  override fun onPlayListChanged(name: String) {
  }

  override fun onServiceConnected(service: MusicService) {
  }

  override fun onMetaChanged() {
  }

  override fun onPlayStateChange() {
  }

  override fun onServiceDisConnected() {
  }

  override fun onTagChanged(
    oldSong: Song,
    newSong: Song
  ) {
  }
}