package remix.myplayer.compose.ui.screen.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.CommonAppBar
import remix.myplayer.compose.ui.widget.app.defaultActions
import remix.myplayer.compose.ui.widget.library.SongListHeader
import remix.myplayer.compose.ui.widget.library.list.ListSong
import remix.myplayer.compose.viewmodel.historyViewModel
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.MusicUtil

@Composable
fun HistoryScreen() {
  val historyVM = historyViewModel
  val songs by historyVM.historySongs.collectAsStateWithLifecycle()
  val musicState by musicViewModel.musicState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      CommonAppBar(
        title = stringResource(R.string.drawer_history),
        actions = {
          HistoryActions {
            historyVM.refreshSortOrder()
          }
        })
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->

    Column(modifier = Modifier.padding(contentPadding)) {
      if (songs.isNotEmpty()) {
        SongListHeader(songs)
      }

      LazyColumn(modifier = Modifier.weight(1f)) {
        itemsIndexed(songs, key = { _, song ->
          song.id
        }) { pos, song ->
          val isPlayingSong = musicState.song.id == song.id

          ListSong(
            modifier = Modifier.height(64.dp),
            song = song,
            modelParent = song,
            selected = false,
            playing = isPlayingSong,
            onClickSong = {
              if (songs.isEmpty()) {
                return@ListSong
              }

              setPlayQueue(
                songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                  .putExtra(MusicService.EXTRA_POSITION, pos)
              )
            },
            onLongClickSong = {
            })
        }
      }
    }
  }
}

@Composable
private fun HistoryActions(onSortOrderChange: () -> Unit) {
  HistoryPopup(onSortOrderChange)

  defaultActions.map { it ->
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