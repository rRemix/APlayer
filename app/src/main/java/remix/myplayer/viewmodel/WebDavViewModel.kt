package remix.myplayer.viewmodel

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
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.Song
import remix.myplayer.repo.WebDavRepository
import remix.myplayer.repo.usecase.FetchMetaDataUseCase
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.state.DataUiState
import remix.myplayer.util.ext.isAudio
import remix.myplayer.util.ext.updateIf
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WebDavViewModel @Inject constructor(
  private val webDavRepository: WebDavRepository,
  private val fetchMetaDataUseCase: FetchMetaDataUseCase
) : ViewModel() {

  private val _webDavList = MutableStateFlow<List<WebDav>>(emptyList())
  val webDavList: StateFlow<List<WebDav>> = _webDavList.asStateFlow()

  private val _webDavResState =
    MutableStateFlow<DataUiState<List<DavResource>>>(DataUiState.Loading())
  val webDavResState: StateFlow<DataUiState<List<DavResource>>> = _webDavResState.asStateFlow()

  init {
    viewModelScope.launch {
      webDavRepository.allWebDav().collect {
        _webDavList.value = it
      }
    }
  }

  fun loadDavRes(sardine: OkHttpSardine, url: String) {
    _webDavResState.value = DataUiState.Loading()
    viewModelScope.launch {
      val resources = try {
        withContext(Dispatchers.IO) {
          sardine.list(url)
        }
      } catch (e: Exception) {
        _webDavResState.value = DataUiState.Error(e)
        return@launch
      }
      _webDavResState.value =
        DataUiState.Success(resources.drop(1).filter { it.isAudio() || it.isDirectory })
    }
  }

  fun deleteWebDav(webDav: WebDav) = viewModelScope.launch {
    webDavRepository.delete(webDav)
  }

  fun updateLastUrl(webDav: WebDav, newUrl: String) = viewModelScope.launch {
    if (webDav.lastUrl == newUrl) {
      return@launch
    }
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
      } else {
        MessageNotifier.show(R.string.add_error)
      }
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

  suspend fun fetchMeta(song: Song.Remote) = fetchMetaDataUseCase(song)
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
