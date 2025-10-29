package remix.myplayer.viewmodel

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.bean.github.Release
import remix.myplayer.data.bean.mp3.APlayerModel
import remix.myplayer.misc.update.DownloadWorker
import remix.myplayer.misc.update.InAppUpdater
import remix.myplayer.misc.updateIf
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoadingResult
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Util
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val inAppUpdater: InAppUpdater
) : ViewModel() {
  private val _inAppUpdateState = MutableStateFlow(InAppUpdateState())
  val inAppUpdateState = _inAppUpdateState.asStateFlow()

  private var inAppUpdateChecked = false

  fun checkInAppUpdate(force: Boolean = false) {
    if (inAppUpdateChecked && !force) {
      return
    }
    inAppUpdater.cancelDownloadWorker()
    inAppUpdateChecked = true
    viewModelScope.launch {
      val release = inAppUpdater.checkUpdate(force)
      Timber.v("checkInAppUpdate release: $release")
      showInAppUpdateDialog(release ?: return@launch)
    }
  }

  fun startDownload(context: Context, release: Release) {
    viewModelScope.launch {

      suspend fun awaitWorkerAndGetPath(): String? {
        return inAppUpdater
          .startDownloadWorker(release)
          .onEach {
            Timber.v("DownloadWorker, state: ${it?.state}")
          }
          .first { info ->
            info?.state == WorkInfo.State.SUCCEEDED
          }?.outputData?.getString(DownloadWorker.EXTRA_FILE_PATH)
      }

      MessageNotifier.show(R.string.downloading)

      val path = if (release.isForceUpdate()) {
        runWithLoadingResult(false, context.getString(R.string.updating)) {
          awaitWorkerAndGetPath()
        }
      } else {
        awaitWorkerAndGetPath()
      }

      if (path.isNullOrEmpty()) {
        return@launch
      }
      Util.installApk(context, path)
    }
  }

  fun showInAppUpdateDialog(release: Release) {
    _inAppUpdateState.updateIf(
      condition = { !_inAppUpdateState.value.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(release = release)
      })
  }

  fun ignoreForever() {
    inAppUpdater.ignoreForever()
  }

  fun ignoreCurrentVersion(release: Release) {
    inAppUpdater.ignoreVersion(inAppUpdater.getOnlineVersionCode(release))
  }

  private val _multiSelectState = MutableStateFlow(MultiSelectState())
  val multiSelectState = _multiSelectState.asStateFlow()

  fun showMultiSelect(
    context: Context,
    where: MultiSelectState.Where,
    initialSelect: APlayerModel
  ) {
    _multiSelectState.updateIf(
      condition = { it.where != where && !it.isShowing() },
      transform = {
        Util.vibrate(context, 50)
        it.copy(
          where = where,
          selectedModels = listOf(initialSelect)
        )
      }
    )
  }

  fun closeMultiSelect() {
    _multiSelectState.update {
      it.copy(
        where = MultiSelectState.Where.None,
        selectedModels = emptyList()
      )
    }
  }

  fun updateMultiSelectModel(model: APlayerModel) {
    _multiSelectState.update {
      val selectedModels = it.selectedModels.toMutableList()
      if (selectedModels.contains(model)) {
        selectedModels.remove(model)
      } else {
        selectedModels.add(model)
      }

      if (selectedModels.isEmpty()) {
        MultiSelectState()
      } else {
        it.copy(selectedModels = selectedModels)
      }
    }
  }

  fun updateMultiSelectModelsAll(models: List<APlayerModel>) {
    _multiSelectState.update {
      it.copy(selectedModels = models)
    }
  }
}

@Stable
data class MultiSelectState(
  val where: Where = Where.None,
  val selectedModels: List<APlayerModel> = emptyList()
) {

  fun isShowing() = where != Where.None

  fun isShowInLibrary() =
    where == Where.Song || where == Where.Album || where == Where.Artist || where == Where.Genre ||
        where == Where.PlayList || where == Where.Folder

  fun isShowInDetail() = where == Where.Detail

  fun isShowInLastAdded() = where == Where.LastAdded

  fun isShowInSearch() = where == Where.Search

  fun selectedModels(target: Where): Set<String> {
    return if (target == where) {
      selectedModels.map { it.getKey() }.toSet()
    } else {
      emptySet<String>()
    }
  }

  enum class Where {
    None,
    Song,
    Album,
    Artist,
    Genre,
    PlayList,
    Folder,
    Detail,
    LastAdded,
    Search
  }
}

@Stable
data class InAppUpdateState(
  val dialogState: DialogState = DialogState(),
  val release: Release? = null
)