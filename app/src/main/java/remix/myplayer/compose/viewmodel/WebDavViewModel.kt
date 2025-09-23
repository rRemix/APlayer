package remix.myplayer.compose.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.compose.repo.WebDavRepository
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.runWithLoading
import remix.myplayer.compose.updateIf
import remix.myplayer.db.room.model.WebDav
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WebDavViewModel @Inject constructor(
  private val webDavRepository: WebDavRepository
) : ViewModel() {

  private val _webDavList = MutableStateFlow<List<WebDav>>(emptyList())
  val webDavList: StateFlow<List<WebDav>> = _webDavList.asStateFlow()

  private val _webDavResources = MutableStateFlow<List<DavResource>>(emptyList())
  val webDavResources: StateFlow<List<DavResource>> = _webDavResources.asStateFlow()

  init {
    viewModelScope.launch {
      webDavRepository.getAll().collect {
        _webDavList.value = it
      }
    }
  }

  fun loadDavResources(
    sardine: OkHttpSardine,
    url: String,
    andThen: (() -> Unit)? = null
  ) {
    viewModelScope.runWithLoading {
      val resources = withContext(Dispatchers.IO) {
        sardine.list(url)
      }

      _webDavResources.value = resources.takeLast(resources.size - 1)

      andThen?.invoke()
    }
  }

  fun deleteWebDav(webDav: WebDav) = viewModelScope.launch {
    webDavRepository.delete(webDav)
  }

  fun updateLastUrl(webDav: WebDav, newUrl: String) = viewModelScope.launch {
//    val updated = webDav.copy(lastUrl = newUrl).also { it.id = webDav.id }
    webDav.lastUrl = newUrl
    webDavRepository.insertOrReplace(webDav)
  }

  fun insertOrReplaceWebDav(webdav: WebDav) = viewModelScope.runWithLoading {
    val sardine = OkHttpSardine()
    sardine.setCredentials(webdav.account, webdav.pwd)
    try {
      val davResources = withContext(Dispatchers.IO) {
        sardine.list(webdav.server)
      }
      if (davResources.isNotEmpty()) {
        webDavRepository.insertOrReplace(webdav)
      }
    } catch (e: Exception) {
      Timber.e(e)
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