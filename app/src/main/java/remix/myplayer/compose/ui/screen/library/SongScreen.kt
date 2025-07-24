package remix.myplayer.compose.ui.screen.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun SongScreen() {
  val libraryVM = libraryViewModel
  val mainVM = mainViewModel

  val musicState by musicViewModel.musicState.collectAsStateWithLifecycle()
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val songs by libraryVM.songs.collectAsStateWithLifecycle()
  val context = LocalContext.current

  Column {
    if (songs.isNotEmpty()) {
      SongListHeader(songs)
    }

    val selectedIds by remember {
      derivedStateOf {
        multiSelectState.selectedModels(MultiSelectState.Where.Song)
      }
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

            if (multiSelectState.where == MultiSelectState.Where.Song) {
              mainVM.updateMultiSelectModel(song)
              return@ListSong
            }

            setPlayQueue(
              songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, pos)
            )
          },
          onLongClickSong = {
            mainVM.showMultiSelect(context, MultiSelectState.Where.Song, song)
          })
      }
    }
  }
}