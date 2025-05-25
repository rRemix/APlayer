package remix.myplayer.compose.viewmodel

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.bean.misc.Library
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.Setting
import remix.myplayer.compose.repo.AlbumRepository
import remix.myplayer.compose.repo.SongRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  private val albumRepo: AlbumRepository,
  val setting: Setting,
//  val appTheme: AppTheme
) : ViewModel() {
  fun loadInit(hasPermission: Boolean) {
    // load libraries
    val libraryJson = setting.libraryJson
    val libraries = if (TextUtils.isEmpty(libraryJson))
      ArrayList()
    else
      Gson().fromJson<java.util.ArrayList<Library>>(
        libraryJson,
        object : TypeToken<List<Library>>() {}.type
      )
    if (libraries.isEmpty()) {
      val defaultLibraries = Library.allLibraries
      libraries.addAll(defaultLibraries)
      setting.libraryJson = Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type)
    }

    setAllLibraries(libraries)

    changeLibrary(libraries[0])

    // load all media
    if (hasPermission) {
      fetchSongs()
      fetchAlbums()
    }
  }

  private val _allLibraries = MutableStateFlow(Library.allLibraries)
  val allLibraries: StateFlow<List<Library>> = _allLibraries

  fun setAllLibraries(libraries: List<Library>) {
    _allLibraries.value = libraries
  }

  private val _currentLibrary = MutableStateFlow<Library>(Library.defaultLibrary)
  val currentLibrary: StateFlow<Library> = _currentLibrary

  fun changeLibrary(library: Library) {
    _currentLibrary.value = library
  }

  private val _songs = MutableStateFlow<List<Song>>(emptyList())
  val songs: StateFlow<List<Song>> = _songs.asStateFlow()

  fun fetchSongs() {
    viewModelScope.launch(Dispatchers.IO) {
      _songs.value = songRepo.allSongs()
    }
  }

  private val _albums = MutableStateFlow<List<Album>>(emptyList())
  val album: StateFlow<List<Album>> = _albums.asStateFlow()

  fun fetchAlbums() = viewModelScope.launch(Dispatchers.IO) {
    _albums.value = albumRepo.allAlbums()
  }
}