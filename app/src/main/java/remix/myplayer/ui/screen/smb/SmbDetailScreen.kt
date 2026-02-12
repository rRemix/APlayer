package remix.myplayer.ui.screen.smb

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.smb.SmbException
import remix.myplayer.data.model.smb.SmbFile
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
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
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.smbViewModel
import timber.log.Timber

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
    smb.buildPathStack(smb.lastUrl).toMutableStateList()
  }
  val currentUrl = pathStack.last()

  var smbFiles by remember {
    mutableStateOf<List<SmbFile>>(emptyList())
  }
  var refreshTrigger by remember {
    mutableIntStateOf(0)
  }

  fun handleBack() {
    if (pathStack.size <= 1) {
      nav.popBackStack()
      return
    }
    pathStack.removeAt(pathStack.lastIndex)
  }

  BackHandler {
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
          smbVM.updateLastUrl(smb, currentUrl)
        }

        is DataUiState.Error -> {
          val ex = (resourceState as DataUiState.Error).throwable
          if (ex is SmbException && ex.isNotFound) {
            if (pathStack.size <= 1) {
              nav.popBackStack()
              MessageNotifier.show(R.string.load_failed)
            } else {
              pathStack.removeRange(1, pathStack.size)
              MessageNotifier.show(R.string.file_not_exist)
            }
          } else {
            nav.popBackStack()
            MessageNotifier.show(R.string.load_failed)
          }
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
                  val nextPath =
                    smb.getRoot().removeSuffix("/") + "/" + resource.path.trimStart('/')
                  Timber.v("nextPath: $nextPath")
                  pathStack.add(nextPath)
                } else {
                  // Filter music and play
                  if (smbFiles.isEmpty()) {
                    return@SmbDetailItem
                  }

                  var select: Song.Remote? = null
                  val remotes = smbFiles
                    .filter { it.isAudio }
                    .map {
                      val uriStr = smb.generateUri(it.path)

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
                val uriStr = smb.generateUri(resource.path)

                val song = Song.Remote(
                  title = resource.name.substringBeforeLast('.'),
                  data = uriStr,
                  size = resource.size,
                  dateModified = resource.lastModified,
                  account = smb.account,
                  pwd = smb.pwd
                )

                when (it) {
                  R.string.add_to_next_song -> {
                    Util.sendLocalBroadcast(
                      MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
                        .putExtra(MusicService.EXTRA_SONG, song)
                    )
                  }

                  R.string.add_to_play_queue -> {
                    playbackVM.insertToQueue(listOf(song))
                  }

                  R.string.song_detail -> {
                    scope.runWithLoading {
                      smbVM.fetchMeta(song)
                      settingVM.showSongDetailDialog(song)
                    }
                  }
                }
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

  LaunchedEffect(currentUrl, refreshTrigger) {
    smbVM.loadSmbRes(smb, currentUrl)
  }
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
    val isAudio = smbFile.isAudio
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

    val list = arrayListOf<Int>()
    if (smbFile.isDirectory) {
      list.add(R.string.delete)
    }

    if (isAudio) {
      list.addAll(
        0,
        listOf(
          R.string.add_to_next_song,
          R.string.add_to_play_queue,
          R.string.song_detail,
          R.string.delete
        )
      )
    }
    PopupButton(list, contentDescription = "SmbDetailPopupButton", onMenuClick = onMenuClick)
  }
}
