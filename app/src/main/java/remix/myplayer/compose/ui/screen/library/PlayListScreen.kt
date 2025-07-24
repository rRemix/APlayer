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
fun PlayListScreen() {
  val libraryVM = libraryViewModel
  val playlists by libraryVM.playLists.collectAsStateWithLifecycle()
  val setting = libraryVM.settingPrefs
  val nav = LocalNavController.current
  var grid by remember { mutableIntStateOf(setting.playlistMode) }

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
      setting.playlistMode = grid
    }

    val selectedIds by remember {
      derivedStateOf {
        multiSelectState.selectedModels(MultiSelectState.Where.PlayList)
      }
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
            selected = selectedIds.contains(pl.getKey()),
            onClick = {
              if (multiSelectState.where == MultiSelectState.Where.PlayList) {
                mainVM.updateMultiSelectModel(pl)
                return@ListItem
              }

              nav.navigate(DetailScreenRoute(playList = pl))
            },
            onLongClick = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.PlayList, pl)
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
          items(playlists, key = {
            it.id
          }) { pl ->
            GridItem(
              pl, text1 = pl.name,
              selected = selectedIds.contains(pl.getKey()),
              onClick = {
                if (multiSelectState.where == MultiSelectState.Where.PlayList) {
                  mainVM.updateMultiSelectModel(pl)
                  return@GridItem
                }

                nav.navigate(DetailScreenRoute(playList = pl))
              },
              onLongClick = {
                mainVM.showMultiSelect(context, MultiSelectState.Where.PlayList, pl)
              })
          }
        })
    }
  }
}