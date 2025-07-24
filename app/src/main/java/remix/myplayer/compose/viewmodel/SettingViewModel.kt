package remix.myplayer.compose.viewmodel

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.prefs.LyricPrefs
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.repo.usecase.DeleteSongUseCase
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.runWithLoading
import remix.myplayer.compose.updateIf
import remix.myplayer.db.room.model.PlayList
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

  private val _addSongToPlayListState =
    MutableStateFlow(ImportPlayListState(DialogState(false), DialogState(false)))
  val addSongToPlayListState = _addSongToPlayListState.asStateFlow()

  fun showAddSongToPlayListDialog(songIds: List<Long>, initialText: String = "") {
    _addSongToPlayListState.updateIf(
      condition = { !it.baseDialogState.isOpen },
      transform = {
        it.baseDialogState.show()
        it.copy(
          inputText = initialText,
          songIds = songIds
        )
      }
    )
  }

  fun updateImportPlayListState(text: String) {
    _addSongToPlayListState.update {
      it.copy(inputText = text)
    }
  }

  private val _deleteSongState =
    MutableStateFlow(DeleteSongState(deleteSource = settingPrefs.deleteSource))
  val deleteSongState = _deleteSongState.asStateFlow()

  fun showDeleteSongDialog(
    models: List<APlayerModel>,
    titleRes: Int = R.string.confirm_delete_from_library,
    parent: APlayerModel? = null
  ) {
    _deleteSongState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(
          models = models,
          titleRes = titleRes,
          parent = parent
        )
      }
    )
  }

  fun updateDeleteSongState(deleteSource: Boolean) {
    _deleteSongState.update {
      it.copy(deleteSource = deleteSource)
    }
  }

  fun deleteSongs(
    activity: BaseActivity?,
    models: List<APlayerModel>,
    deleteSource: Boolean,
    parent: APlayerModel?
  ) =
    viewModelScope.runWithLoading {
      deleteSongUseCase(activity, models, deleteSource, parent)
    }

  private val _reNamePlayListState = MutableStateFlow(ReNamePlayListState(DialogState()))
  val reNamePlayListState = _reNamePlayListState.asStateFlow()

  fun showReNamePlayListDialog(playList: PlayList) {
    _reNamePlayListState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(playList = playList)
      }
    )
    if (!_reNamePlayListState.value.dialogState.isOpen) {
      _reNamePlayListState.value.dialogState.show()
    }
  }

  private val _songDetailState = MutableStateFlow(SongDetailState(DialogState()))
  val songDetailState = _songDetailState.asStateFlow()

  fun showSongDetailDialog(song: Song) {
    _songDetailState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(song = song)
      }
    )
  }

  private val _songEditState = MutableStateFlow(SongEditState(DialogState()))
  val songEditState = _songEditState.asStateFlow()

  fun showSongEditDialog(song: Song) {
    _songEditState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(song = song)
      }
    )
  }
}

@Stable
data class SongEditState(val dialogState: DialogState, val song: Song? = null)

@Stable
data class SongDetailState(val dialogState: DialogState, val song: Song = Song.EMPTY_SONG)

@Stable
data class ReNamePlayListState(
  val dialogState: DialogState,
  val playList: PlayList? = null,
)

@Stable
data class DeleteSongState(
  val dialogState: DialogState = DialogState(),
  val titleRes: Int = R.string.confirm_delete_from_library,
  val models: List<APlayerModel> = emptyList(),
  val deleteSource: Boolean = false,
  val parent: APlayerModel? = null
)

@Stable
data class ImportPlayListState(
  val baseDialogState: DialogState,
  val inputDialogState: DialogState,
  val inputText: String = "",
  val songIds: List<Long> = emptyList()
)