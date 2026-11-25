package remix.myplayer.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.remote.RemoteFileSystemFactory
import remix.myplayer.repo.WebDavRepository
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.ext.updateIf
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WebDavViewModel @Inject constructor(
  private val webDavRepository: WebDavRepository,
  private val fileSystems: RemoteFileSystemFactory
) : ViewModel() {

  private val _webDavList = MutableStateFlow<List<WebDav>>(emptyList())
  val webDavList: StateFlow<List<WebDav>> = _webDavList.asStateFlow()

  init {
    viewModelScope.launch {
      webDavRepository.allWebDav().collect {
        _webDavList.value = it
      }
    }
  }

  fun deleteWebDav(webDav: WebDav) = viewModelScope.launch {
    webDavRepository.delete(webDav)
  }

  fun insertOrReplaceWebDav(webdav: WebDav) = viewModelScope.runWithLoading {
    try {
      fileSystems.create(webdav).checkConnection()
      webDavRepository.insertOrReplace(webdav)
    } catch (e: Exception) {
      Timber.e(e)
      MessageNotifier.show(e.localizedMessage ?: "Save failed")
    }
  }

  private val _addWebDavState = MutableStateFlow(AddWebDavState(DialogState()))
  val addWebDavState = _addWebDavState.asStateFlow()

  fun updateAddWebDavState(
    alias: String? = null,
    account: String? = null,
    pwd: String? = null,
    server: String? = null
  ) {
    _addWebDavState.update {
      it.copy(
        alias = alias ?: it.alias,
        account = account ?: it.account,
        pwd = pwd ?: it.pwd,
        server = server ?: it.server
      )
    }
  }

  fun showAddWebDavDialog(editWebDav: WebDav? = null) {
    _addWebDavState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(
          alias = editWebDav?.alias ?: it.alias,
          account = editWebDav?.account ?: it.account,
          pwd = editWebDav?.pwd ?: it.pwd,
          server = editWebDav?.server ?: it.server,
          editWebDav = editWebDav
        )
      }
    )
  }

}

@Stable
data class AddWebDavState(
  val dialogState: DialogState,
  val editWebDav: WebDav? = null,
  val alias: String = "",
  val account: String = "",
  val pwd: String = "",
  val server: String = ""
)
