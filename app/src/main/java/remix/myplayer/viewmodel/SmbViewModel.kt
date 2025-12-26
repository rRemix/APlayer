package remix.myplayer.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.repo.SmbRepository
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.state.DataUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SmbViewModel @Inject constructor(
  private val smbRepository: SmbRepository
) : ViewModel() {

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

  fun loadSmbRes(smb: Smb, path: String) {
    _smbResState.value = DataUiState.Loading()
    viewModelScope.launch {
      apiCall(smb, path)
    }
  }

  private suspend fun apiCall(smb: Smb, path: String) {
    try {
      withContext(Dispatchers.IO) {
        val client = SMBClient()
        val connection = client.connect(smb.server)
        val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
        val session = connection.authenticate(authContext)
        val diskShare = session.connectShare(smb.share) as DiskShare

        val fileInfos = diskShare.list(path)
        val files = fileInfos.map {
          SmbFile(
            name = it.fileName,
            isDirectory = it.fileAttributes_Long and 16L != 0L, // Directory attribute
            path = if (path.endsWith("\\")) path + it.fileName else "$path\\${it.fileName}",
            size = it.endOfFile,
            lastModified = it.changeTime.toEpochMillis()
          )
        }.filter { it.name != "." && it.name != ".." }
        
        // Close resources
        diskShare.close()
        session.close()
        connection.close()
        client.close()

        _smbResState.value = DataUiState.Success(files)
      }
    } catch (e: Exception) {
      Timber.e(e)
      _smbResState.value = DataUiState.Error(e)
    }
  }

  fun deleteSmb(smb: Smb) = viewModelScope.launch {
    smbRepository.delete(smb)
  }

  fun updateLastPath(smb: Smb, newPath: String) = viewModelScope.launch {
    if (smb.lastPath == newPath) {
      return@launch
    }
    smb.lastPath = newPath
    smbRepository.insertOrReplace(smb)
  }

  fun insertOrReplaceSmb(smb: Smb) = viewModelScope.runWithLoading {
    try {
      withContext(Dispatchers.IO) {
        val client = SMBClient()
        val connection = client.connect(smb.server)
        val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
        val session = connection.authenticate(authContext)
        val diskShare = session.connectShare(smb.share) as DiskShare
        // If successful
        diskShare.close()
        session.close()
        connection.close()
        client.close()
      }
      smbRepository.insertOrReplace(smb)
    } catch (e: Exception) {
      Timber.e(e)
      // Signal error to UI? For now just log
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
        editSmb = editSmb
      )
    }
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
  val share: String = ""
)

data class SmbFile(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val size: Long,
    val lastModified: Long
)
