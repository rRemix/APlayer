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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.prefs.LyricPrefs
import remix.myplayer.compose.prefs.SettingPrefs
import javax.inject.Inject

// TODO 将**Logic中的变量放在这里
@HiltViewModel
class SettingViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  val settingPrefs: SettingPrefs,
  val lyricPrefs: LyricPrefs,
) : ViewModel() {

  private val _currentLibrary = MutableStateFlow(Library.defaultLibrary)
  val currentLibrary: StateFlow<Library> = _currentLibrary

  private val _allLibraries = MutableStateFlow(Library.allLibraries)
  val allLibraries: StateFlow<List<Library>> = _allLibraries

  init {
    // load libraries
    val libraryJson = settingPrefs.libraryJson
    val libraries = if (TextUtils.isEmpty(libraryJson))
      ArrayList()
    else
      Gson().fromJson<ArrayList<Library>>(
        libraryJson,
        object : TypeToken<List<Library>>() {}.type
      )
    if (libraries.isEmpty()) {
      val defaultLibraries = Library.allLibraries
      libraries.addAll(defaultLibraries)
      settingPrefs.libraryJson =
        Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type)
    }

    setAllLibraries(libraries)

    changeLibrary(libraries[0])

    viewModelScope.launch {
      // 自动保存配置的Libraries
      _allLibraries.collect {
        settingPrefs.libraryJson = Gson().toJson(it, object : TypeToken<List<Library>>() {}.type)
      }
    }
  }

  fun setAllLibraries(libraries: List<Library>) {
    _allLibraries.value = libraries
  }

  fun changeLibrary(library: Library) {
    _currentLibrary.value = library
  }
}