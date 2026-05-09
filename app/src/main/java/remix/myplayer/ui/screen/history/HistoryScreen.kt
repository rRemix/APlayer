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
import remix.myplayer.helper.SleepTimer
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.AppBarAction
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.library.SongListHeader
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.util.MusicUtil
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.timerViewModel

@Composable
fun HistoryScreen() {
  val items by libraryViewModel.historySongs.collectAsStateWithLifecycle()
  val songs = items.map { it.first }
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
        itemsIndexed(items, key = { _, item ->
          item.first.id
        }) { pos, item ->
          val song = item.first
          val isPlayingSong = playbackState.song.id == song.id

          ListSong(
            modifier = Modifier.height(64.dp),
            song = song,
            modelParent = song,
            selected = false,
            playing = isPlayingSong,
            num = item.second,
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

  val timerVM = timerViewModel
  val libraryVM = libraryViewModel
  val timerRunning by SleepTimer.runningState.collectAsStateWithLifecycle()
  val timerIcon = if (timerRunning) R.drawable.ic_timer_on_24dp else R.drawable.ic_timer_white_24dp

  listOf(
    AppBarAction(timerIcon, "Timer") {
      timerVM.showTimerDialog()
    },
    AppBarAction(R.drawable.ic_delete_black_24dp, "ClearHistory") {
      libraryVM.clearHistory()
    }).forEach {
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
