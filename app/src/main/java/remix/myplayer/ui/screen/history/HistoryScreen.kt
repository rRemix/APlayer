package remix.myplayer.ui.screen.history

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
import remix.myplayer.misc.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.defaultAppBarActions
import remix.myplayer.ui.widget.library.SongListHeader
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.util.MusicUtil
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.playbackViewModel

@Composable
fun HistoryScreen() {
  val songs by libraryViewModel.historySongs.collectAsStateWithLifecycle()
  val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      CommonAppBar(
        title = stringResource(R.string.drawer_history),
        actions = {
          HistoryActions()
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
          val isPlayingSong = playbackState.song.id == song.id

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
                songs, MusicUtil.makeCmdIntent(Command.PLAY_AT)
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
private fun HistoryActions() {
  HistoryPopup()

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