package remix.myplayer.viewmodel

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.github.Release
import remix.myplayer.misc.update.DownloadWorker
import remix.myplayer.misc.update.InAppUpdater
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoadingResult
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Util
import remix.myplayer.util.ext.updateIf
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val inAppUpdater: Lazy<InAppUpdater>
) : ViewModel() {

  private val _openPlayingScreenRequests = MutableStateFlow(false)
  val openPlayingScreenRequests = _openPlayingScreenRequests.asStateFlow()

  fun requestPlayingScreen() {
    _openPlayingScreenRequests.value = true
  }

  fun consumePlayingScreenRequest() {
    _openPlayingScreenRequests.value = false
  }

  private val _inAppUpdateState = MutableStateFlow(InAppUpdateState())
  val inAppUpdateState = _inAppUpdateState.asStateFlow()

  private var inAppUpdateChecked = false

  fun checkInAppUpdate(force: Boolean = false) {
    if (!BuildConfig.ENABLE_UPDATE) {
      return
    }
    if (inAppUpdateChecked && !force) {
      return
    }
    inAppUpdater.get().cancelDownloadWorker()
    inAppUpdateChecked = true
    viewModelScope.launch {
      val release = inAppUpdater.get().checkUpdate(force)
      Timber.v("checkInAppUpdate release: $release")
      showInAppUpdateDialog(release ?: return@launch)
    }
  }

  fun startDownload(context: Context, release: Release) {
    if (!BuildConfig.ENABLE_UPDATE) {
      return
    }
    viewModelScope.launch {

      suspend fun awaitWorkerAndGetPath(): String? {
        return inAppUpdater.get()
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
    if (!BuildConfig.ENABLE_UPDATE) {
      return
    }
    inAppUpdater.get().ignoreForever()
  }

  fun ignoreCurrentVersion(release: Release) {
    if (!BuildConfig.ENABLE_UPDATE) {
      return
    }
    val updater = inAppUpdater.get()
    updater.ignoreVersion(updater.getOnlineVersionCode(release))
  }

  private val _multiSelectState = MutableStateFlow(MultiSelectState())
  val multiSelectState = _multiSelectState.asStateFlow()

  fun showMultiSelect(
    context: Context,
    where: MultiSelectState.Where,
    initialSelect: APlayerModel,
    owner: String? = null
  ) {
    _multiSelectState.updateIf(
      condition = { !it.isShowing() || (where == MultiSelectState.Where.Remote &&
        it.where == where && it.owner != owner) },
      transform = {
        Util.vibrate(context, 50)
        it.copy(
          where = where,
          owner = owner,
          selectedModels = listOf(initialSelect)
        )
      }
    )
  }

  fun closeMultiSelect() {
    _multiSelectState.update {
      it.copy(
        where = MultiSelectState.Where.None,
        owner = null,
        selectedModels = emptyList()
      )
    }
  }

  fun closeRemoteSelection(owner: String) {
    _multiSelectState.update {
      if (it.isShowInRemote(owner)) MultiSelectState() else it
    }
  }

  fun updateMultiSelectModel(model: APlayerModel) {
    _multiSelectState.update {
      val modelKey = model.getKey()
      val selectedModels = it.selectedModels.toMutableList()
      val removed = selectedModels.removeAll { selected ->
        selected.getKey() == modelKey
      }
      if (!removed) {
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
      it.copy(selectedModels = models.distinctBy { model -> model.getKey() })
    }
  }
}

@Stable
data class MultiSelectState(
  val where: Where = Where.None,
  val owner: String? = null,
  val selectedModels: List<APlayerModel> = emptyList()
) {

  fun isShowing() = where != Where.None

  fun isShowInLibrary() =
    where == Where.Song || where == Where.Album || where == Where.Artist || where == Where.Genre ||
        where == Where.PlayList || where == Where.Folder

  fun isShowInDetail() = where == Where.Detail

  fun isShowInLastAdded() = where == Where.LastAdded

  fun isShowInSearch() = where == Where.Search

  fun isShowInRemote(owner: String) = where == Where.Remote && this.owner == owner

  fun selectedModels(target: Where): Set<String> {
    return if (target == where) {
      selectedModels.map { it.getKey() }.toSet()
    } else {
      emptySet()
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
    Search,
    Remote
  }
}

@Stable
data class InAppUpdateState(
  val dialogState: DialogState = DialogState(),
  val release: Release? = null
)
