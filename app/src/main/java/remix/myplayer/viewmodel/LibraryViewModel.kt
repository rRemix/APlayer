package remix.myplayer.viewmodel

import remix.myplayer.data.model.audio.SongIdentity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.util.Constants
import remix.myplayer.R
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.coil.CoverUriResolver
import remix.myplayer.repo.AlbumRepository
import remix.myplayer.repo.ArtistRepository
import remix.myplayer.repo.FolderRepository
import remix.myplayer.repo.GenreRepository
import remix.myplayer.repo.HistoryRepository
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.SongQuery
import remix.myplayer.repo.MediaStoreLibrary
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.observe
import remix.myplayer.repo.usecase.ExportPlayListUseCase
import remix.myplayer.repo.usecase.PlayFromUriUseCase
import remix.myplayer.service.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.PermissionUtil
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @param:ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  private val mediaStoreLibrary: MediaStoreLibrary,
  private val albumRepo: AlbumRepository,
  private val artistRepo: ArtistRepository,
  private val genreRepo: GenreRepository,
  private val playListRepo: PlayListRepository,
  private val folderRepo: FolderRepository,
  private val coverUriResolver: CoverUriResolver,
  private val historyRepo: HistoryRepository,
  val settingPrefs: SettingPrefs,
  private val exportPlayListUseCase: ExportPlayListUseCase,
  private val playFromUriUseCase: PlayFromUriUseCase
) : ViewModel(), MusicEventCallback {

  private var hasPermission = false
  private val sharing = SharingStarted.WhileSubscribed(5000)
  val songs = observeSongs().stateIn(viewModelScope, sharing, emptyList())
  val albums = albumRepo.observeAlbums().stateIn(viewModelScope, sharing, emptyList())
  val artists = artistRepo.observeArtists().stateIn(viewModelScope, sharing, emptyList())
  val genres = genreRepo.observeGenres().stateIn(viewModelScope, sharing, emptyList())
  val folders = folderRepo.observeFolders().stateIn(viewModelScope, sharing, emptyList())
  val playLists = playListRepo.allPlayLists().stateIn(viewModelScope, sharing, emptyList())

  val historySongs = combine(historyRepo.allHistories(), songRepo.observeSongs()) { histories, _ ->
    val resolved = songRepo.resolveSongs(histories.map { it.identity() }).associateBy { it.songIdentity }
    histories.mapNotNull { history -> resolved[history.identity()]?.let { it to history.play_count } }
  }.stateIn(viewModelScope, sharing, emptyList())

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
    hasPermission = PermissionUtil.hasNecessaryPermission()
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

  fun addSongsToPlayList(keys: List<SongIdentity>, playListName: String, createNew: Boolean = false) {
    viewModelScope.launch {
      try {
        if (createNew) {
          if (playListRepo.checkPlayListExist(playListName)) {
            MessageNotifier.show(R.string.playlist_already_exist)
            return@launch
          }

          playListRepo.insertPlayList(playListName)
        }

        val count = playListRepo.addSongsToPlayList(keys, playListName = playListName)
        MessageNotifier.show(R.string.add_song_playlist_success, count, playListName)
      } catch (ignore: Exception) {
        MessageNotifier.show(R.string.add_song_playlist_error)
      }
    }
  }

  suspend fun loadSongs(items: List<APlayerModel>) = songRepo.getSongs(items)

  suspend fun loadSongs(query: SongQuery) = songRepo.getSongs(query)

  fun observeSongs(query: SongQuery = SongQuery()): Flow<List<Song>> = songRepo
    .observeSongs(query.copy(sortOrder = null))
    .combine(settingPrefs.observe(PrefKeys.Setting.SONG_SORT_ORDER) { songSortOrder }) { items, order ->
      ItemsSorter.sortedSongs(items, order)
    }.flowOn(Dispatchers.Default)

  fun updateSongOrder(id: Long, keys: List<SongIdentity>) {
    viewModelScope.launch {
      playListRepo.updateSongOrder(id, keys)
      MessageNotifier.show(R.string.save_success)
    }
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

  fun refreshCovers() = coverUriResolver.refreshMemory()

  fun invalidateCover(type: Int, id: Long) {
    val key = when (type) {
      Constants.ALBUM -> "album:$id"
      Constants.ARTIST -> "artist:$id"
      Constants.PLAYLIST -> "playlist:$id"
      else -> return
    }
    coverUriResolver.invalidate(key)
  }

  fun fetchMedia() {
    viewModelScope.launch { mediaStoreLibrary.refresh() }
  }

  fun clearHistory() = viewModelScope.launch {
    historyRepo.clear()
  }

  override fun onMediaStoreChanged() {
    // MusicService synchronizes this event once; all library lists observe its published snapshot.
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
    // MusicService refreshes the snapshot before replacing the tagged song in playback.
  }
}

data class CreatePlaylistState(
  val dialogState: DialogState = DialogState(),
  val name: String = ""
)
