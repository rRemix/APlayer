package remix.myplayer.compose.ui.widget.library.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import remix.myplayer.compose.spanCount
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.library.ModeHeader
import remix.myplayer.compose.ui.widget.library.list.GridItem
import remix.myplayer.compose.ui.widget.library.list.ListAlbum
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.ui.adapter.HeaderAdapter

@Composable
fun AlbumScreen() {
  val libraryVM: LibraryViewModel = viewModel()
  val albums by libraryVM.album.collectAsStateWithLifecycle()
  val setting = libraryVM.setting

  var grid by rememberSaveable { mutableIntStateOf(setting.albumMode) }

  Column(
    modifier = Modifier.background(LocalTheme.current.libraryBackground)) {
    ModeHeader(grid == HeaderAdapter.GRID_MODE) {
      if (grid == it) {
        return@ModeHeader
      }
      grid = if (grid == HeaderAdapter.GRID_MODE) HeaderAdapter.LIST_MODE else HeaderAdapter.GRID_MODE
      setting.albumMode = grid
    }
    if (grid == HeaderAdapter.LIST_MODE) {
      LazyColumn {
        itemsIndexed(albums, key = { index, album ->
          album.albumID
        }) { pos, album ->
          ListAlbum(album = album)
        }
      }
    } else {
      LazyVerticalGrid(
        modifier = Modifier,
        columns = GridCells.Fixed(spanCount()),
        content = {
          items(albums, key = {
            it.albumID
          }) {
            GridItem(it, it.album, it.artist, onClick = {}, onLongClick = {})
          }
        })
    }
  }
}
