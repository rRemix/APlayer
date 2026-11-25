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
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.remote.RemoteFileSystemFactory
import remix.myplayer.repo.SmbRepository
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SmbViewModel @Inject constructor(
  private val smbRepository: SmbRepository,
  private val fileSystems: RemoteFileSystemFactory
) : ViewModel() {

  private val _smbList = MutableStateFlow<List<Smb>>(emptyList())
  val smbList: StateFlow<List<Smb>> = _smbList.asStateFlow()

  init {
    viewModelScope.launch {
      smbRepository.allSmb().collect {
        _smbList.value = it
      }
    }
  }

  fun deleteSmb(smb: Smb) = viewModelScope.launch {
    smbRepository.delete(smb)
  }

  fun insertOrReplaceSmb(smb: Smb) = viewModelScope.runWithLoading {
    try {
      fileSystems.create(smb).checkConnection()
      smbRepository.insertOrReplace(smb)
    } catch (e: Exception) {
      Timber.e(e)
      MessageNotifier.show(e.localizedMessage ?: "Save failed")
    }
  }

  private val _addSmbState = MutableStateFlow(AddSmbState(DialogState()))
  val addSmbState = _addSmbState.asStateFlow()

  fun updateAddSmbState(
    alias: String? = null,
    domain: String? = null,
    account: String? = null,
    pwd: String? = null,
    server: String? = null,
    share: String? = null
  ) {
    _addSmbState.update {
      it.copy(
        alias = alias ?: it.alias,
        domain = domain ?: it.domain,
        account = account ?: it.account,
        pwd = pwd ?: it.pwd,
        server = server ?: it.server,
        share = share ?: it.share
      )
    }
  }

  fun showAddSmbDialog(editSmb: Smb? = null) {
    _addSmbState.update {
      it.dialogState.show()
      it.copy(
        alias = editSmb?.alias ?: it.alias,
        domain = editSmb?.domain ?: it.domain,
        account = editSmb?.account ?: it.account,
        pwd = editSmb?.pwd ?: it.pwd,
        server = editSmb?.server ?: it.server,
        share = editSmb?.share ?: it.share,
        editSmb = editSmb,
        availableShares = emptyList(),
        isLoadingShares = false,
        showShareSelection = false
      )
    }
  }

  fun dismissShareSelection() {
    _addSmbState.update { it.copy(showShareSelection = false) }
  }

}

@Stable
data class AddSmbState(
  val dialogState: DialogState,
  val editSmb: Smb? = null,
  val alias: String = "",
  val domain: String = "",
  val account: String = "",
  val pwd: String = "",
  val server: String = "",
  val share: String = "",
  val availableShares: List<String> = emptyList(),
  val isLoadingShares: Boolean = false,
  val showShareSelection: Boolean = false
)
