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
fun ArtistScreen() {
  val libraryVM = libraryViewModel
  val artists by libraryVM.artists.collectAsStateWithLifecycle()
  val setting = libraryVM.settingPrefs
  val nav = LocalNavController.current
  var grid by remember { mutableIntStateOf(setting.artistMode) }

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
      setting.artistMode = grid
    }

    val selectedIds by remember {
      derivedStateOf {
        multiSelectState.selectedModels(MultiSelectState.Where.Artist)
      }
    }

    if (grid == HeaderAdapter.LIST_MODE) {
      val listState = rememberLazyListState()
      LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
        itemsIndexed(artists, key = { index, artist ->
          artist.artistID
        }) { pos, artist ->
          ListItem(
            modifier = Modifier.height(64.dp),
            model = artist,
            text1 = artist.artist,
            selected = selectedIds.contains(artist.getKey()),
            text2 = pluralStringResource(R.plurals.song_num, artist.count, artist.count),
            onClick = {
              if (multiSelectState.where == MultiSelectState.Where.Artist) {
                mainVM.updateMultiSelectModel(artist)
                return@ListItem
              }

              nav.navigate(DetailScreenRoute(artist = artist))
            },
            onLongClick = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.Artist, artist)
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
          items(artists, key = {
            it.artistID
          }) { artist ->
            GridItem(
              artist, text1 = artist.artist,
              selected = selectedIds.contains(artist.getKey()),
              onClick = {
                if (multiSelectState.where == MultiSelectState.Where.Artist) {
                  mainVM.updateMultiSelectModel(artist)
                  return@GridItem
                }
                nav.navigate(DetailScreenRoute(artist = artist))
              },
              onLongClick = {
                mainVM.showMultiSelect(context, MultiSelectState.Where.Artist, artist)
              })
          }
        })
    }
  }
}