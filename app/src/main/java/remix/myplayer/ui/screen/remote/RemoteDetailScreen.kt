package remix.myplayer.ui.screen.remote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import remix.myplayer.R
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.song
import remix.myplayer.data.remote.RemoteBrowserSource
import remix.myplayer.data.remote.RemoteFile
import remix.myplayer.data.remote.RemoteFileException
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.state.DataUiState
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.icon
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.common.AppBarAction
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.PopupButton
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.RemoteBrowserViewModel
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDetailScreen(
  source: RemoteBrowserSource,
  browserVM: RemoteBrowserViewModel = hiltViewModel()
) {
  val nav = LocalNavController.current
  val context = LocalContext.current
  val mainVM = mainViewModel
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val playbackVM = playbackViewModel
  val settingVM = settingViewModel
  val libraryVM = libraryViewModel
  val scope = rememberCoroutineScope()

  val pathStack = rememberSaveable(
    source.key,
    saver = listSaver(
      save = { it.toList() },
      restore = { it.toMutableStateList() })
  ) {
    source.buildPathStack().toMutableStateList()
  }
  val currentUrl = pathStack.last()
  val selectionOwner = "${source.key}|$currentUrl"
  // A pending request must not survive switching to another source or folder.
  val deleteConfirmation = remember(source.key, currentUrl) { RemoteDeleteConfirmation() }
  val deleteDialogState = remember(source.key, currentUrl) { DialogState() }
  val showMultiSelect = multiSelectState.isShowInRemote(selectionOwner)
  val selectedKeys = if (showMultiSelect) {
    multiSelectState.selectedModels(MultiSelectState.Where.Remote)
  } else {
    emptySet()
  }

  var refreshTrigger by remember {
    mutableIntStateOf(0)
  }

  val resourceState by produceState<DataUiState<List<RemoteFile>>>(
    DataUiState.Loading(), source.key, currentUrl, refreshTrigger
  ) {
    value = DataUiState.Loading()
    try {
      val files = browserVM.list(source, currentUrl)
      value = DataUiState.Success(files)
      // Failure to persist the last folder must not discard a successful listing.
      try {
        browserVM.updateLastUrl(source, currentUrl)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        timber.log.Timber.w(e, "Failed to save remote last URL")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      value = DataUiState.Error(e)
    }
  }
  val resources = (resourceState as? DataUiState.Success)?.data.orEmpty()
  val songsByPath = remember(resources, source.key) {
    val musicSource = source.musicSource
    resources.filter { it.isAudio }.associate { resource ->
      resource.path to musicSource.song(
        resource.path, resource.name, resource.size,
        resource.lastModified
      )
    }
  }
  val showLoading = resourceState is DataUiState.Loading

  fun requestDelete(resources: List<RemoteFile>) {
    deleteConfirmation.request(resources)
    if (deleteConfirmation.pending != null) deleteDialogState.show()
  }

  fun deleteResources(resources: List<RemoteFile>) {
    if (resources.isEmpty()) return
    scope.runWithLoading(onError = { MessageNotifier.show(R.string.delete_error) }) {
      try {
        resources.forEach { resource ->
          browserVM.delete(source, resource)
        }
      } finally {
        // Earlier successful deletes must be reflected even if a later request fails.
        refreshTrigger++
      }
    }
  }

  DisposableEffect(mainVM, selectionOwner) {
    onDispose {
      mainVM.closeRemoteSelection(selectionOwner)
    }
  }

  fun handleBack() {
    if (showMultiSelect) {
      mainVM.closeMultiSelect()
      return
    }
    if (pathStack.size <= 1) {
      nav.popBackStack()
      return
    }
    pathStack.removeAt(pathStack.lastIndex)
  }

  BackHandler(enabled = pathStack.size > 1 || showMultiSelect) {
    handleBack()
  }

  deleteConfirmation.pending?.let { request ->
    val message = when {
      request.files.size == 1 && request.files[0].isDirectory ->
        stringResource(R.string.remote_confirm_delete_directory, request.files[0].name)
      request.files.size == 1 ->
        stringResource(R.string.remote_confirm_delete_file, request.files[0].name)
      request.directoryCount > 0 ->
        stringResource(R.string.remote_confirm_delete_with_directories,
          request.files.size, request.directoryCount)
      else -> stringResource(R.string.remote_confirm_delete_multiple, request.files.size)
    }
    NormalDialog(
      dialogState = deleteDialogState,
      title = stringResource(R.string.remote_confirm_delete_title),
      content = message,
      onDismissRequest = { deleteConfirmation.cancel() },
      onNegative = { deleteConfirmation.cancel() },
      onPositive = {
        val confirmed = deleteConfirmation.confirm()
        if (confirmed.isNotEmpty()) {
          mainVM.closeRemoteSelection(selectionOwner)
          deleteResources(confirmed)
        }
      }
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.systemBars,
    topBar = {
      AnimatedContent(
        targetState = showMultiSelect,
        transitionSpec = {
          if (targetState) slideInVertically() togetherWith slideOutVertically { it / 2 }
          else slideInVertically { it } togetherWith slideOutVertically()
        },
        label = "remoteSelection"
      ) { selecting ->
        if (selecting) {
          MultiSelectBar(
            state = multiSelectState,
            scrollBehavior = null,
            onSelectAll = { mainVM.updateMultiSelectModelsAll(songsByPath.values.toList()) },
            onDelete = { selected ->
              val keys = selected.mapTo(hashSetOf()) { it.getKey() }
              requestDelete(resources.filter { songsByPath[it.path]?.getKey() in keys })
            }
          )
        } else {
          CommonAppBar(
            title = source.alias,
            onBack = { handleBack() },
            actions = listOf(AppBarAction(R.drawable.ic_close_white_24dp, "RemoteDetailClose") {
              nav.popBackStack()
            })
          )
        }
      }
    },
    containerColor = LocalTheme.current.mainBackground
  ) { contentPadding ->
    LaunchedEffect(resourceState, currentUrl) {
      val error = (resourceState as? DataUiState.Error)?.throwable ?: return@LaunchedEffect
      if (error is RemoteFileException.NotFound) {
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

    Column(modifier = Modifier.padding(contentPadding)) {
      Box(modifier = Modifier.weight(1f)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(resources, key = { it.path }) { resource ->
            val song = songsByPath[resource.path]
            RemoteDetailItem(
              resource,
              selected = song?.getKey() in selectedKeys,
              enabled = !showLoading,
              popupEnabled = !showMultiSelect && !showLoading,
              onLongClick = song?.let { selectedSong ->
                { mainVM.showMultiSelect(context, MultiSelectState.Where.Remote, selectedSong, selectionOwner) }
              },
              onClick = {
                if (showLoading) return@RemoteDetailItem

                if (showMultiSelect) {
                  song?.let(mainVM::updateMultiSelectModel)
                  return@RemoteDetailItem
                }
                if (resource.isDirectory) {
                  pathStack.add(RemotePaths.resolve(source.root, resource.path))
                } else if (song != null) {
                  val remotes = songsByPath.values.toList()
                  MusicServiceRemote.setPlayQueue(
                    remotes, MusicUtil.makeCmdIntent(Command.PLAY_AT)
                      .putExtra(MusicService.EXTRA_POSITION, remotes.indexOf(song))
                  )
                }
              },
              onMenuClick = {
                if (it == R.string.delete) {
                  requestDelete(listOf(resource))
                  return@RemoteDetailItem
                }
                val song = songsByPath[resource.path] ?: return@RemoteDetailItem
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

                  R.string.add_to_playlist -> {
                    settingVM.showAddSongToPlayListDialog(listOf(song.songIdentity))
                  }

                  R.string.collect -> {
                    libraryVM.playLists.value.firstOrNull { it.isFavorite() }?.let { favorite ->
                      libraryVM.addSongsToPlayList(listOf(song.songIdentity), favorite.name)
                    }
                  }

                  R.string.song_detail -> {
                    settingVM.showSongDetailDialog(song)
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

}

@Composable
private fun RemoteDetailItem(
  remoteFile: RemoteFile,
  selected: Boolean,
  enabled: Boolean,
  popupEnabled: Boolean,
  onLongClick: (() -> Unit)?,
  onClick: () -> Unit,
  onMenuClick: (Int) -> Unit
) {
  val theme = LocalTheme.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .combinedClickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = theme.ripple),
        onClick = onClick,
        onLongClick = onLongClick
      )
      .background(if (selected) theme.select else theme.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val isAudio = remoteFile.isAudio
    val icon = if (remoteFile.isDirectory) {
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
      contentDescription = "IconRemoteDetailItem",
      tint = theme.icon()
    )

    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .weight(1f),
      horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(remoteFile.name)
      Spacer(Modifier.height(4.dp))
      TextSecondary(remoteFile.path)
    }

    val list = arrayListOf(R.string.delete)
    if (remoteFile.isAudio) {
      list.addAll(
        0,
        listOf(
          R.string.add_to_next_song,
          R.string.add_to_play_queue,
          R.string.add_to_playlist,
          R.string.collect,
          R.string.song_detail,
        )
      )
    }
    if (popupEnabled) {
      PopupButton(list, contentDescription = "RemoteDetailPopupButton", onMenuClick = onMenuClick)
    } else {
      Spacer(Modifier.size(48.dp))
    }
  }
}
