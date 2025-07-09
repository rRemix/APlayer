package remix.myplayer.compose.viewmodel

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.prefs.LyricPrefs
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.repo.usecase.DeleteSongUseCase
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.runWithLoading
import javax.inject.Inject

// TODO 将**Logic中的变量放在这里
@HiltViewModel
class SettingViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  val settingPrefs: SettingPrefs,
  val lyricPrefs: LyricPrefs,
) : ViewModel() {
  @Inject
  lateinit var deleteSongUseCase: DeleteSongUseCase

  private val _currentLibrary = MutableStateFlow(Library.defaultLibrary)
  val currentLibrary = _currentLibrary.asStateFlow()

  private val _allLibraries = MutableStateFlow(Library.allLibraries)
  val allLibraries = _allLibraries.asStateFlow()

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

  private val _importPlayListState =
    MutableStateFlow(ImportPlayListState(DialogState(false), DialogState(false)))
  val importPlayListState = _importPlayListState.asStateFlow()

  fun showImportPlayListDialog(songIds: List<Long>, initialText: String = "") {
    _importPlayListState.value.baseDialogState.show()

    _importPlayListState.value = _importPlayListState.value.copy(
      inputText = initialText,
      songIds = songIds
    )
  }

  fun updateImportPlayListState(text: String) {
    _importPlayListState.value = _importPlayListState.value.copy(
      inputText = text
    )
  }

  private val _deleteSongState =
    MutableStateFlow(DeleteSongState(deleteSource = settingPrefs.deleteSource))
  val deleteSongState = _deleteSongState.asStateFlow()

  fun showDeleteSongDialog(
    models: List<APlayerModel>,
    titleRes: Int = R.string.confirm_delete_from_library,
  ) {
    if (!_deleteSongState.value.dialogState.isOpen) {
      _deleteSongState.value.dialogState.show()
    }

    _deleteSongState.value = _deleteSongState.value.copy(
      models = models,
      titleRes = titleRes,
    )
  }

  fun updateDeleteSongState(deleteSource: Boolean) {
    _deleteSongState.value = _deleteSongState.value.copy(
      deleteSource = deleteSource,
    )
  }

  fun deleteSongs(activity: BaseActivity?, models: List<APlayerModel>, deleteSource: Boolean) =
    viewModelScope.runWithLoading {
      deleteSongUseCase(activity, models, deleteSource)
    }

  private val _reNamePlayListState = MutableStateFlow(ReNamePlayListState(DialogState()))
  val reNamePlayListState = _reNamePlayListState.asStateFlow()

  fun showReNamePlayListDialog(id: Long) {
    if (!_reNamePlayListState.value.dialogState.isOpen) {
      _reNamePlayListState.value.dialogState.show()
    }

    _reNamePlayListState.value = _reNamePlayListState.value.copy(playListId = id)
  }
}

@Stable
data class ReNamePlayListState(
  val dialogState: DialogState,
  val playListId: Long = 0L,
)

@Stable
data class DeleteSongState(
  val dialogState: DialogState = DialogState(),
  val titleRes: Int = R.string.confirm_delete_from_library,
  val models: List<APlayerModel> = emptyList(),
  val deleteSource: Boolean = false
)

@Stable
data class ImportPlayListState(
  val baseDialogState: DialogState,
  val inputDialogState: DialogState,
  val inputText: String = "",
  val songIds: List<Long> = emptyList()
)