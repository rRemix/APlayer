package remix.myplayer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.SortOrder
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.library.GlideCover
import remix.myplayer.util.Util
import remix.myplayer.util.ext.clickableWithoutRipple
import remix.myplayer.viewmodel.libraryViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CustomSortScreen(id: Long) {
  val libraryVM = libraryViewModel

  val nav = LocalNavController.current
  val context = LocalContext.current

  var playList by remember {
    mutableStateOf<PlayList?>(null)
  }
  val songs = remember {
    mutableStateListOf<Song>()
  }

  Scaffold(
    topBar = {
      CommonAppBar(title = playList?.name ?: "", actions = emptyList())
    },
    floatingActionButton = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(color = LocalTheme.current.secondary, shape = CircleShape)
          .clickableWithoutRipple {
            val playList = playList ?: return@clickableWithoutRipple

            val newIds = songs.map { it.id }
            if (newIds != playList.audioIds) {
              libraryVM.settingPrefs.setPlayListDetailSortOrder(
                playList.id,
                SortOrder.PLAYLIST_SONG_CUSTOM
              )

              libraryVM.updatePlayList(playList.copy(audioIds = ArrayList(newIds)))
            }

            nav.popBackStack()
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(R.drawable.ic_save_white_24dp),
          contentDescription = "CustomSortSave",
          tint = Color.White
        )
      }
    }
  ) { padding ->
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
      songs.add(to.index, songs.removeAt(from.index))
    }

    LazyColumn(
      modifier = Modifier
        .padding(padding)
        .background(LocalTheme.current.libraryBackground),
      state = lazyListState
    ) {
      itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
        ReorderableItem(reorderableLazyListState, key = song.id) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .background(LocalTheme.current.mainBackground),
            verticalAlignment = Alignment.CenterVertically
          ) {
            GlideCover(
              modifier = Modifier
                .padding(8.dp)
                .size(42.dp),
              model = song,
              circle = false
            )

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              TextPrimary(song.title)
              TextSecondary(song.album)
            }

            Icon(
              painter = painterResource(R.drawable.ic_drag_handle_24dp),
              contentDescription = "CustomSortDragHandle",
              tint = LocalTheme.current.textSecondary,
              modifier = Modifier
                .padding(end = 8.dp)
                .draggableHandle(
                  onDragStarted = {
                    Util.vibrate(context, 50)
                  },
                  onDragStopped = {
                    Util.vibrate(context, 50)
                  }
                )
            )
          }
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    playList = libraryVM.playLists.value.first { it.id == id }

    val result = withContext(Dispatchers.IO) {
      libraryVM.loadSongsByModels(listOf(playList!!))
    }
    songs.clear()
    songs.addAll(result)
  }

}
