package remix.myplayer.compose.ui.screen.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.widget.library.SongListHeader
import remix.myplayer.compose.ui.widget.library.list.ListSong
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.MusicUtil

@Composable
fun SongScreen(vm: LibraryViewModel = activityViewModel()) {
  val songs by vm.songs.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  Column {
    if (songs.isNotEmpty()) {
      SongListHeader(songs)
    }

    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
      itemsIndexed(songs, key = { _, song ->
        song.id
      }) { pos, song ->
        ListSong(
          modifier = Modifier.height(64.dp),
          song = song,
          onClickSong = {
            if (songs.isEmpty()) {
              return@ListSong
            }
            setPlayQueue(
              songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, pos)
            )
          },
          onLongClickSong = {})
      }
    }
  }
}