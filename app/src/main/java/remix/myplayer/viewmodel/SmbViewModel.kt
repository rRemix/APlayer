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
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.smb.SmbClientDelegateProvider
import remix.myplayer.data.model.smb.SmbFile
import remix.myplayer.misc.manager.DynamicModuleManager
import remix.myplayer.misc.manager.DynamicModuleStatus
import remix.myplayer.repo.SmbRepository
import remix.myplayer.repo.usecase.FetchMetaDataUseCase
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.state.DataUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SmbViewModel @Inject constructor(
  private val smbRepository: SmbRepository,
  private val fetchMetaDataUseCase: FetchMetaDataUseCase,
  private val delegateProvider: SmbClientDelegateProvider,
  private val dynamicModuleManager: DynamicModuleManager
) : ViewModel() {

  val supportSmb get() = dynamicModuleManager.isModuleSupport("feature_smb")

  val isSmbModuleInstalled get() = dynamicModuleManager.isModuleInstalled("feature_smb")

  fun installSmbModule() = dynamicModuleManager.installModule("feature_smb")

  private val _moduleInstallStatus = MutableStateFlow<DynamicModuleStatus?>(null)
  val moduleInstallStatus = _moduleInstallStatus.asStateFlow()

  fun startSmbModuleInstallation() {
    viewModelScope.launch {
      installSmbModule().collect {
        _moduleInstallStatus.value = it
      }
    }
  }

  fun clearInstallStatus() {
    _moduleInstallStatus.value = null
  }

  private val _smbList = MutableStateFlow<List<Smb>>(emptyList())
  val smbList: StateFlow<List<Smb>> = _smbList.asStateFlow()

  private val _smbResState = MutableStateFlow<DataUiState<List<SmbFile>>>(DataUiState.Loading())
  val smbResState: StateFlow<DataUiState<List<SmbFile>>> = _smbResState.asStateFlow()

  init {
    viewModelScope.launch {
      smbRepository.allSmb().collect {
        _smbList.value = it
      }
    }
  }

  fun loadSmbRes(smb: Smb, url: String) {
    _smbResState.value = DataUiState.Loading()
    viewModelScope.launch {
      apiCall(smb, url)
    }
  }

  private suspend fun apiCall(smb: Smb, url: String) {
    val d = delegateProvider.getDelegate()
    if (d == null) {
      _smbResState.value = DataUiState.Error(Exception("SMB module not installed"))
      return
    }
    try {
      val files = d.listFiles(smb, url)
      _smbResState.value = DataUiState.Success(files)
    } catch (e: Exception) {
      Timber.e(e)
      _smbResState.value = DataUiState.Error(e)
    }
  }

  fun deleteSmb(smb: Smb) = viewModelScope.launch {
    smbRepository.delete(smb)
  }

  fun updateLastUrl(smb: Smb, newUrl: String) = viewModelScope.launch {
    if (smb.lastUrl == newUrl) {
      return@launch
    }
    smb.lastUrl = newUrl
    smbRepository.insertOrReplace(smb)
  }

  fun insertOrReplaceSmb(smb: Smb) = viewModelScope.runWithLoading {
    val d = delegateProvider.getDelegate()
    if (d == null) {
      MessageNotifier.show("SMB module not installed")
      return@runWithLoading
    }
    try {
      d.checkConnection(smb)
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

  suspend fun fetchMeta(song: Song.Remote) = fetchMetaDataUseCase(song)
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
