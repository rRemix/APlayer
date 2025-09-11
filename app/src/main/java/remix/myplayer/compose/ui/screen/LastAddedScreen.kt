package remix.myplayer.compose.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.CommonAppBar
import remix.myplayer.compose.ui.widget.app.MultiSelectBar
import remix.myplayer.compose.ui.widget.app.defaultActions
import remix.myplayer.compose.ui.widget.library.SongListHeader
import remix.myplayer.compose.ui.widget.library.list.ListSong
import remix.myplayer.compose.viewmodel.MultiSelectState
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.mainViewModel
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.MusicUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastAddedScreen() {
  val libraryVM = libraryViewModel
  val mainVM = mainViewModel

  val musicState by musicViewModel.musicState.collectAsStateWithLifecycle()
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val songs = remember {
    mutableStateListOf<Song>()
  }
  val context = LocalContext.current

  val showMultiSelect = multiSelectState.isShowInLastAdded()

  BackPressHandler(showMultiSelect) {
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
          CommonAppBar(title = stringResource(R.string.recently), actions = defaultActions)
        } else {
          MultiSelectBar(
            state = multiSelectState,
            scrollBehavior = null,
          )
        }
      }
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->

    Column(modifier = Modifier.padding(contentPadding)) {
      val selectedIds by remember {
        derivedStateOf {
          multiSelectState.selectedModels(MultiSelectState.Where.LastAdded)
        }
      }

      if (songs.isNotEmpty()) {
        SongListHeader(songs)
      }

      LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
        itemsIndexed(songs, key = { _, song ->
          song.id
        }) { pos, song ->
          val selected = selectedIds.contains(song.getKey())
          val isPlayingSong = musicState.song.id == song.id

          ListSong(
            modifier = Modifier.height(64.dp),
            song = song,
            modelParent = song,
            selected = selected,
            playing = isPlayingSong,
            onClickSong = {
              if (songs.isEmpty()) {
                return@ListSong
              }

              if (multiSelectState.where == MultiSelectState.Where.LastAdded) {
                mainVM.updateMultiSelectModel(song)
                return@ListSong
              }

              setPlayQueue(
                songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                  .putExtra(MusicService.EXTRA_POSITION, pos)
              )
            },
            onLongClickSong = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.LastAdded, song)
            })
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      songs.addAll(libraryVM.loadLastAddedSongs())
    }
  }

}