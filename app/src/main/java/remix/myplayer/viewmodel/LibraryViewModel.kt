package remix.myplayer.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Audio
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.glide.UriFetcher
import remix.myplayer.repo.AlbumRepository
import remix.myplayer.repo.ArtistRepository
import remix.myplayer.repo.FolderRepository
import remix.myplayer.repo.GenreRepository
import remix.myplayer.repo.HistoryRepository
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.usecase.ExportPlayListUseCase
import remix.myplayer.repo.usecase.PlayFromUriUseCase
import remix.myplayer.service.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @param:ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  private val albumRepo: AlbumRepository,
  private val artistRepo: ArtistRepository,
  private val genreRepo: GenreRepository,
  private val playListRepo: PlayListRepository,
  private val folderRepo: FolderRepository,
  private val uriFetcher: UriFetcher,
  private val historyRepo: HistoryRepository,
  val settingPrefs: SettingPrefs,
  private val exportPlayListUseCase: ExportPlayListUseCase,
  private val playFromUriUseCase: PlayFromUriUseCase
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

  val playLists = playListRepo.allPlayLists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _folders = MutableStateFlow<List<Folder>>(emptyList())
  val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

  val historySongs = historyRepo.allHistories().map { histories ->
    histories.mapNotNull { history ->
      val song = withContext(Dispatchers.IO) { songRepo.song(history.audio_id) }
      song?.let { it to history.play_count }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  private val _createPlaylistState = MutableStateFlow(CreatePlaylistState())
  val createPlaylistState = _createPlaylistState.asStateFlow()

  fun showCreatePlaylistDialog() {
    val defaultName = "${context.getString(R.string.local_list)}${playLists.value.size}"
    _createPlaylistState.update {
      it.dialogState.show()
      it.copy(name = defaultName)
    }
  }

  fun updateNewPlaylistName(name: String) {
    _createPlaylistState.update { it.copy(name = name) }
  }

  init {
    // load all media
    hasPermission = PermissionUtil.hasNecessaryPermission()
    if (hasPermission) {
      fetchMedia()
    }
  }

  fun insertPlayList(name: String, onSuccess: (Long) -> Unit) {
    viewModelScope.launch {
      if (playListRepo.checkPlayListExist(name)) {
        MessageNotifier.show(R.string.playlist_already_exist)
        return@launch
      }

      val id = playListRepo.insertPlayList(name)
      onSuccess(id)
    }
  }

  fun addSongsToPlayList(audioIds: List<Long>, playListName: String, createNew: Boolean = false) {
    viewModelScope.launch {
      try {
        if (createNew) {
          if (playListRepo.checkPlayListExist(playListName)) {
            MessageNotifier.show(R.string.playlist_already_exist)
            return@launch
          }

          playListRepo.insertPlayList(playListName)
        }

        val count = playListRepo.addSongsToPlayList(audioIds, playListName = playListName)
        MessageNotifier.show(R.string.add_song_playlist_success, count, playListName)
      } catch (ignore: Exception) {
        MessageNotifier.show(R.string.add_song_playlist_error)
      }
    }
  }

  suspend fun loadSongsByModels(models: List<APlayerModel>) = songRepo.getSongsByModels(models)

  fun loadSong(selection: String?, selectionValues: Array<String?>?, sortOrder: String? = null) =
    songRepo.getSongs(selection, selectionValues, sortOrder)

  fun loadLastAddedSongs() = songRepo.getLastAddedSongs()

  fun searchSong(key: String): List<Song> {
    checkWorkerThread()
    val likeKey = "%$key%"
    return songRepo.getSongs(
      "(" +
          Audio.Media.TITLE + " LIKE ? OR " +
          Audio.ArtistColumns.ARTIST + " LIKE ? OR " +
          Audio.AlbumColumns.ALBUM + " LIKE ? OR " +
          Audio.Media.DISPLAY_NAME + " LIKE ?" +
          ")",
      arrayOf(likeKey, likeKey, likeKey, likeKey),
      settingPrefs.songSortOrder
    )
  }

  fun updatePlayList(playList: PlayList) {
    viewModelScope.launch {
      try {
        val duplicate = playLists.value.find { it.name == playList.name && it.id != playList.id }
        if (duplicate != null) {
          MessageNotifier.show(R.string.playlist_already_exist)
          return@launch
        }

        playListRepo.updatePlayList(playList)
        uriFetcher.updatePlayListVersion()
        uriFetcher.clearAllCache()
        Glide.get(context).clearMemory()
        MessageNotifier.show(R.string.save_success)
      } catch (e: Exception) {
        MessageNotifier.show(R.string.save_error)
      }
    }
  }

  fun exportPlayListToFile(playList: PlayList?, uri: Uri) {
    viewModelScope.launch {
      exportPlayListUseCase(playList, uri)
    }
  }

  fun playFromUri(uri: Uri) {
    viewModelScope.launch {
      playFromUriUseCase(uri)
    }
  }

  fun fetchMedia(
    clear: Boolean = false,
    updateAlbumVersion: Boolean = false,
    updateArtistVersion: Boolean = false,
    updatePlayListVersion: Boolean = false
  ) {
    viewModelScope.launch {
      if (clear) {
        if (updateAlbumVersion) {
          uriFetcher.updateAlbumVersion()
        } else if (updateArtistVersion) {
          uriFetcher.updateArtistVersion()
        } else if (updatePlayListVersion) {
          uriFetcher.updatePlayListVersion()
        } else {
          uriFetcher.updateAllVersion()
        }
        uriFetcher.clearAllCache()
        Glide.get(context).clearMemory()
      }

      _songs.value = async(Dispatchers.IO) { songRepo.allSongs() }.await()
      _albums.value = async(Dispatchers.IO) { albumRepo.allAlbums() }.await()
      _artists.value = async(Dispatchers.IO) { artistRepo.allArtists() }.await()
      _genres.value = async(Dispatchers.IO) { genreRepo.allGenres() }.await()
      _folders.value = async(Dispatchers.IO) { folderRepo.allFolders() }.await()
      Timber.v("songCount: ${_songs.value.size} albumCount: ${_albums.value.size} artistCount: ${_artists.value.size} genreCount: ${_genres.value.size} folderCount: ${_folders.value.size}")
    }
  }

  fun clearHistory() = viewModelScope.launch {
    historyRepo.clear()
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

  override fun onServiceDisConnected() {
  }

  override fun onTagChanged(
    oldSong: Song?, newSong: Song
  ) {
    fetchMedia(true, updateAlbumVersion = true, updatePlayListVersion = true)
  }
}

data class CreatePlaylistState(
  val dialogState: DialogState = DialogState(),
  val name: String = ""
)
