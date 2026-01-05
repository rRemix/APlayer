package remix.myplayer.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import remix.myplayer.data.model.audio.Song
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
  private val fetchMetaDataUseCase: FetchMetaDataUseCase
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

  fun loadSmbRes(smb: Smb, url: String) {
    _smbResState.value = DataUiState.Loading()
    viewModelScope.launch {
      apiCall(smb, url)
    }
  }

  private suspend fun apiCall(smb: Smb, url: String) {
    try {
      withContext(Dispatchers.IO) {
        SMBClient().use { client ->
          val (host, port) = Smb.parseServerAddress(smb.server)
          val connection = if (port != null) client.connect(host, port) else client.connect(host)
          connection.use {
            val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
            val session = connection.authenticate(authContext)
            session.use {
              val diskShare = session.connectShare(smb.share) as DiskShare
              diskShare.use { share ->
                val relativePath = smb.getRelativePath(url).replace('/', '\\')

                val fileInfos = share.list(relativePath)
                val files = fileInfos.map {
                  val fileName = it.fileName
                  val fileRelativePath = if (relativePath.isEmpty()) fileName else "$relativePath\\$fileName"
                  SmbFile(
                    name = fileName,
                    isDirectory = (it.fileAttributes and 16L) != 0L,
                    path = fileRelativePath.replace('\\', '/'),
                    size = it.endOfFile,
                    lastModified = it.changeTime.toEpochMillis()
                  )
                }.filter { it.name != "." && it.name != ".." }.filter {
                  it.isDirectory || it.isAudio
                }
                _smbResState.value = DataUiState.Success(files)
              }
            }
          }
        }
      }
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
    try {
      withContext(Dispatchers.IO) {
        SMBClient().use { client ->
          val (host, port) = Smb.parseServerAddress(smb.server)
          val connection = if (port != null) client.connect(host, port) else client.connect(host)
          connection.use {
            val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
            val session = connection.authenticate(authContext)
            session.use {
              val diskShare = session.connectShare(smb.share) as DiskShare
              diskShare.use {
                // Just to check if connection works
              }
            }
          }
        }
      }
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

data class SmbFile(
  val name: String,
  val isDirectory: Boolean,
  val path: String,
  val size: Long,
  val lastModified: Long
) {

  val isAudio: Boolean
    get() {
      val lower = name.lowercase()
      return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(
        ".m4a"
      ) || lower.endsWith(".ogg")
    }
}