package remix.myplayer.ui.screen.smb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.model.audio.Song
import remix.myplayer.misc.helper.MusicServiceRemote
import remix.myplayer.misc.isAudio
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.clickWithRipple
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.BackPressHandler
import remix.myplayer.ui.state.DataUiState
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.icon
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.common.AppBarAction
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.PopupButton
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.SmbFile
import remix.myplayer.viewmodel.SmbViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.smbViewModel

@Composable
fun SmbDetailScreen(smb: Smb) {
  val nav = LocalNavController.current
  val smbVM = smbViewModel
  val playbackVM = playbackViewModel
  val settingVM = settingViewModel
  val scope = rememberCoroutineScope()
  val resourceState by smbVM.smbResState.collectAsStateWithLifecycle()

  val pathStack = rememberSaveable(
    saver = listSaver(
      save = { it.toList() },
      restore = { it.toMutableStateList() })
  ) {
    val initial = if (smb.lastPath.isNotEmpty()) {
        val parts = smb.lastPath.split("\\").filter { it.isNotEmpty() }
        val list = mutableListOf<String>()
        var current = ""
        parts.forEach { part ->
            current = if (current.isEmpty()) part else "$current\\$part"
            list.add(current)
        }
        list
    } else {
        listOf("")
    }
    initial.toMutableStateList()
  }
  val currentPath = if (pathStack.isEmpty()) "" else pathStack.last()

  var smbFiles by remember {
    mutableStateOf<List<SmbFile>>(emptyList())
  }
  var refreshTrigger by remember {
    mutableIntStateOf(0)
  }

  fun handleBack() {
    if (pathStack.isEmpty() || (pathStack.size == 1 && pathStack.first() == "")) {
      nav.popBackStack()
      return
    }
    pathStack.removeAt(pathStack.lastIndex)
  }

  BackPressHandler {
    handleBack()
  }

  Scaffold(
    topBar = {
      CommonAppBar(
        title = smb.alias,
        onBack = {
          handleBack()
        },
        actions = listOf(AppBarAction(R.drawable.ic_close_white_24dp, "SmbDetailClose") {
          nav.popBackStack()
        })
      )
    },
    containerColor = LocalTheme.current.mainBackground
  ) { contentPadding ->
    val showLoading = resourceState is DataUiState.Loading

    LaunchedEffect(resourceState) {
      when (resourceState) {
        is DataUiState.Success -> {
          smbFiles = resourceState.get()
          smbVM.updateLastPath(smb, currentPath)
        }

        is DataUiState.Error -> {
           val ex = (resourceState as DataUiState.Error).throwable
           MessageNotifier.show(ex.message ?: "Load failed")
           // TODO: Handle more specific errors, like disconnected or auth failed, maybe pop back
        }

        else -> {}
      }
    }

    Column(modifier = Modifier.padding(contentPadding)) {
      Box(modifier = Modifier.weight(1f)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(smbFiles, key = { it.path }) { resource ->
            SmbDetailItem(
              resource,
              onClick = {
                if (showLoading) return@SmbDetailItem

                if (resource.isDirectory) {
                  // Enter directory
                  pathStack.add(resource.path)
                } else {
                  // Filter music and play
                  if (smbFiles.isEmpty()) {
                    return@SmbDetailItem
                  }
                  
                  var select: Song.Remote? = null
                  val remotes = smbFiles
                    .filter { isAudio(it.name) } // Helper? Need to verify isAudio extension or logic
                     // I can check file extension.
                     // WebDav uses DavResource.isAudio() which checks content type or extension.
                     // I will implement simple check.
                    .map {
                        // Construct URI
                        // smb://[domain;]username[:password]@server/share/path
                        var userInfo = ""
                        if (!smb.domain.isNullOrEmpty()) {
                            userInfo += "${smb.domain};"
                        }
                        userInfo += smb.account
                        if (smb.pwd.isNotEmpty()) {
                            userInfo += ":${smb.pwd}"
                        }
                        
                        // Need to encode userInfo if it contains special chars?
                        // For simplicity, assume basic chars for now. 
                        // URI construction:
                        val uriStr = "smb://$userInfo@${smb.server}/${smb.share}/${it.path.replace("\\", "/")}"

                        val remote = Song.Remote(
                            title = it.name.substringBeforeLast('.'),
                            data = uriStr,
                            size = it.size,
                            dateModified = it.lastModified,
                            account = smb.account,
                            pwd = smb.pwd
                      )
                      if (it == resource) {
                        select = remote
                      }
                      remote
                    }
                    
                  if (remotes.isNotEmpty()) {
                      MusicServiceRemote.setPlayQueue(
                        remotes,
                        MusicUtil.makeCmdIntent(Command.PLAY_AT)
                          .putExtra(MusicService.EXTRA_POSITION, remotes.indexOfFirst {
                            it.data == select?.data
                          })
                      )
                  }
                }
              },
              onMenuClick = {
                  // Similar menu to WebDav
              })
          }
        }

        if (showLoading) {
          LinearProgressIndicator(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter),
            color = LocalTheme.current.primary
          )
        }
      }
      BottomBar()
    }
  }

  LaunchedEffect(currentPath, refreshTrigger) {
    smbVM.loadSmbRes(smb, currentPath)
  }
}

private fun isAudio(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".ogg")
}

@Composable
private fun SmbDetailItem(
  smbFile: SmbFile,
  onClick: () -> Unit,
  onMenuClick: (Int) -> Unit
) {
  val theme = LocalTheme.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .clickWithRipple(false) {
        onClick()
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    val isAudio = isAudio(smbFile.name)
    val icon = if (smbFile.isDirectory) {
      R.drawable.ic_folder_24dp
    } else if (isAudio) {
      R.drawable.ic_audio_file_24dp
    } else {
      R.drawable.ic_lab_profile_24dp
    }

    Icon(
      modifier = Modifier
        .padding(start = 12.dp),
      painter = painterResource(icon),
      contentDescription = "IconSmbDetailItem",
      tint = theme.icon()
    )

    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .weight(1f),
      horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(smbFile.name)
      Spacer(Modifier.height(4.dp))
      TextSecondary(smbFile.path)
    }

    val list = arrayListOf<Int>() // R.string.delete
    // Keep it simple for now, maybe add delete later if implemented in VM
    
    if (isAudio) {
      list.addAll(
        listOf(
          // R.string.add_to_next_song,
          // R.string.add_to_play_queue, 
          // R.string.song_detail,
          // TODO: Implement these action handlers if needed
        )
      )
    }
    // PopupButton(list, contentDescription = "SmbDetailPopupButton", onMenuClick = onMenuClick)
  }
}
