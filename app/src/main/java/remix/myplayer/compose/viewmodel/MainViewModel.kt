package remix.myplayer.compose.viewmodel

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.bean.github.Release
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.misc.update.InAppUpdater
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val inAppUpdater: InAppUpdater
) : ViewModel() {

  private val _inAppUpdateState = MutableStateFlow(InAppUpdateState())
  val inAppUpdateState = _inAppUpdateState.asStateFlow()

  fun checkInAppUpdate(force: Boolean = false) {
    viewModelScope.launch {
      val release = inAppUpdater.checkUpdate(force)
      Timber.v("checkInAppUpdate release: $release")
      showInAppUpdateDialog(release ?: return@launch)
    }
  }

  fun showInAppUpdateDialog(release: Release) {
    if (!_inAppUpdateState.value.dialogState.isOpen) {
      _inAppUpdateState.value.dialogState.show()
    }
    _inAppUpdateState.value = _inAppUpdateState.value.copy(release = release)
  }

  fun ignoreForever() {
    inAppUpdater.ignoreForever()
  }

  fun ignoreCurrentVersion(release: Release) {
    inAppUpdater.ignoreVersion(inAppUpdater.getOnlineVersionCode(release))
  }
}

@Stable
data class InAppUpdateState(
  val dialogState: DialogState = DialogState(),
  val release: Release? = null
)