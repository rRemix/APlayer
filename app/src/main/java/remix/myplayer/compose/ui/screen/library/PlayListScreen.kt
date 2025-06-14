package remix.myplayer.compose.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.spanCount
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.library.ModeHeader
import remix.myplayer.compose.ui.widget.library.list.GridItem
import remix.myplayer.compose.ui.widget.library.list.ListItem
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.ui.adapter.HeaderAdapter

@Composable
fun PlayListScreen(vm: LibraryViewModel = activityViewModel()) {
  val playlists by vm.playLists.collectAsStateWithLifecycle()
  val setting = vm.setting

  var grid by rememberSaveable { mutableIntStateOf(setting.playlistMode) }

  Column(
    modifier = Modifier.background(LocalTheme.current.libraryBackground)
  ) {
    ModeHeader(grid == HeaderAdapter.GRID_MODE) {
      if (grid == it) {
        return@ModeHeader
      }
      grid =
        if (grid == HeaderAdapter.GRID_MODE) HeaderAdapter.LIST_MODE else HeaderAdapter.GRID_MODE
      setting.playlistMode = grid
    }
    if (grid == HeaderAdapter.LIST_MODE) {
      val listState = rememberLazyListState()
      LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
        itemsIndexed(playlists, key = { index, pl ->
          pl.id
        }) { pos, pl ->
          ListItem(
            modifier = Modifier.height(64.dp),
            model = pl,
            text1 = pl.name,
            text2 = pluralStringResource(R.plurals.song_num, pl.audioIds.size, pl.audioIds.size),
            onClick = {},
            onLongClick = {})
        }
      }
    } else {
      val gridState = rememberLazyGridState()
      LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        state = gridState,
        columns = GridCells.Fixed(spanCount()),
        content = {
          items(playlists, key = {
            it.id
          }) {
            GridItem(it, text1 = it.name, onClick = {}, onLongClick = {})
          }
        })
    }
  }
}