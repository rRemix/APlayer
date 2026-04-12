package remix.myplayer.ui.screen.detail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.Command
import remix.myplayer.service.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.common.defaultAppBarActions
import remix.myplayer.ui.widget.library.SongListHeader
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ext.verticalScrollbar
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.PlayingScreenValue
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.playbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(model: APlayerModel) {
  val libraryVM = libraryViewModel
  val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()

  val mainVM = mainViewModel
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val playLists by libraryVM.playLists.collectAsStateWithLifecycle()

  val songs = remember {
    mutableStateListOf<Song>()
  }

  var refreshKey by remember {
    mutableIntStateOf(0)
  }

  val showMultiSelect = multiSelectState.isShowInDetail()
  val popupEnabled = !showMultiSelect

  BackHandler(showMultiSelect) {
    mainVM.closeMultiSelect()
  }

  Scaffold(
    topBar = {
      AnimatedContent(
        targetState = showMultiSelect,
        transitionSpec = {
          if (targetState) {
            slideInVertically() togetherWith slideOutVertically { height -> height / 2 }
          } else {
            slideInVertically { height -> height } togetherWith slideOutVertically()
          }
        }
      ) { isMultiSelect ->
        if (!isMultiSelect) {
          CommonAppBar(title = "", actions = {
            DetailActions(model) {
              refreshKey++
            }
          })
        } else {
          MultiSelectBar(
            state = multiSelectState,
            scrollBehavior = null,
            parent = model,
          )
        }
      }
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->

    Column(
      modifier = Modifier.padding(contentPadding),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (songs.isNotEmpty()) {
        SongListHeader(songs)
      }

      val selectedIds by remember {
        derivedStateOf {
          multiSelectState.selectedModels(MultiSelectState.Where.Detail)
        }
      }

      val state = rememberLazyListState()
      LazyColumn(
        state = state,
        modifier = Modifier
          .weight(1f)
          .verticalScrollbar(state)
      ) {
        itemsIndexed(songs, key = { index, song -> "${index}_${song.id}" }) { index, song ->
          val selected = selectedIds.contains(song.getKey())
          val isPlayingSong = playbackState.song.id == song.id

          ListSong(
            modifier = Modifier.height(64.dp),
            song = song,
            modelParent = model,
            selected = selected,
            playing = isPlayingSong,
            popupEnabled = popupEnabled,
            onClickSong = {
              if (multiSelectState.where == MultiSelectState.Where.Detail) {
                mainVM.updateMultiSelectModel(song)
                return@ListSong
              }

              if (playbackState.isPlaying && isPlayingSong) {
                scope.launch {
                  mainVM.playingScreenState.animateTo(PlayingScreenValue.Expanded)
                }
              } else {
                setPlayQueue(
                  songs, MusicUtil.makeCmdIntent(Command.PLAY_AT)
                    .putExtra(MusicService.EXTRA_POSITION, index)
                )
              }
            },
            onLongClickSong = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.Detail, song)
            })
        }
      }
      TextSecondary(
        pluralStringResource(R.plurals.song_num, songs.size, songs.size),
        modifier = Modifier.padding(vertical = 8.dp),
        fontSize = 12.sp
      )
      BottomBar()
    }
  }

  LaunchedEffect(refreshKey) {
    val result = withContext(Dispatchers.IO) {
      libraryVM.loadSongsByModels(listOf(model))
    }
    songs.clear()
    songs.addAll(result)
  }

  if (model is PlayList) {
    LaunchedEffect(playLists) {
      val updatedPlayList = playLists.find { it.id == model.id }
      if (updatedPlayList != null && updatedPlayList.audioIds != model.audioIds) {
        model.audioIds.clear()
        model.audioIds.addAll(updatedPlayList.audioIds)
        refreshKey++
      }
    }
  }

  (LocalActivity.current as? BaseMusicActivity)?.let { activity ->
    DisposableEffect(Unit) {
      val listener = object : MusicEventCallback {
        override fun onMediaStoreChanged() {
          refreshKey++
        }

        override fun onPermissionChanged(has: Boolean) {}

        override fun onPlayListChanged(name: String) {}

        override fun onServiceConnected(service: MusicService) {}

        override fun onServiceDisConnected() {}

        override fun onTagChanged(oldSong: Song?, newSong: Song) {}
      }
      activity.addMusicServiceEventListener(listener)

      onDispose {
        activity.removeMusicServiceEventListener(listener)
      }
    }
  }
}

@Composable
private fun DetailActions(model: APlayerModel, onSortOrderChange: () -> Unit) {
  DetailPopupButton(model, onSortOrderChange)

  defaultAppBarActions.map { it ->
    IconButton(onClick = {
      it.action()
    }) {
      Icon(
        painter = painterResource(it.icon),
        contentDescription = it.contentDescription
      )
    }
  }
}
