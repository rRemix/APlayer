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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.nav.DetailScreenRoute
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.spanCount
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.library.ModeHeader
import remix.myplayer.compose.ui.widget.library.list.GridItem
import remix.myplayer.compose.ui.widget.library.list.ListItem
import remix.myplayer.compose.viewmodel.MultiSelectState
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.mainViewModel
import remix.myplayer.ui.adapter.HeaderAdapter

@Composable
fun AlbumScreen() {
  val libraryVM = libraryViewModel
  val albums by libraryVM.albums.collectAsStateWithLifecycle()
  val setting = libraryVM.settingPrefs
  val nav = LocalNavController.current
  var grid by remember { mutableIntStateOf(setting.albumMode) }

  val mainVM = mainViewModel
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  Column(
    modifier = Modifier.background(LocalTheme.current.libraryBackground)
  ) {
    ModeHeader(grid == HeaderAdapter.GRID_MODE) {
      if (grid == it) {
        return@ModeHeader
      }
      grid =
        if (grid == HeaderAdapter.GRID_MODE) HeaderAdapter.LIST_MODE else HeaderAdapter.GRID_MODE
      setting.albumMode = grid
    }

    val selectedIds by remember {
      derivedStateOf {
        multiSelectState.selectedModels(MultiSelectState.Where.Album)
      }
    }

    if (grid == HeaderAdapter.LIST_MODE) {
      val listState = rememberLazyListState()
      LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
        itemsIndexed(albums, key = { index, album ->
          album.albumID
        }) { pos, album ->
          ListItem(
            modifier = Modifier.height(64.dp),
            model = album,
            text1 = album.album,
            text2 = pluralStringResource(
              R.plurals.song_num_1, album.count, album.artist,
              album.count
            ),
            selected = selectedIds.contains(album.getKey()),
            onClick = {
              if (multiSelectState.where == MultiSelectState.Where.Album) {
                mainVM.updateMultiSelectModel(album)
                return@ListItem
              }
              nav.navigate(DetailScreenRoute(album = album))
            },
            onLongClick = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.Album, album)
            })
        }
      }
    } else {
      val gridState = rememberLazyGridState()
      LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        state = gridState,
        columns = GridCells.Fixed(spanCount()),
        content = {
          items(albums, key = {
            it.albumID
          }) { album ->
            GridItem(
              album, album.album, album.artist,
              selected = selectedIds.contains(album.getKey()),
              onClick = {
                if (multiSelectState.where == MultiSelectState.Where.Album) {
                  mainVM.updateMultiSelectModel(album)
                  return@GridItem
                }

                nav.navigate(DetailScreenRoute(album = album))
              }, onLongClick = {
                mainVM.showMultiSelect(context, MultiSelectState.Where.Album, album)
              })
          }
        })
    }
  }
}
