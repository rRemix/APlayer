package remix.myplayer.ui.screen.library

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.ui.nav.DetailScreenRoute
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.library.ModeHeader
import remix.myplayer.ui.widget.library.list.GridItem
import remix.myplayer.ui.widget.library.list.ListItem
import remix.myplayer.util.ext.spanCount
import remix.myplayer.util.ext.verticalScrollbar
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun ArtistScreen() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
  val nav = LocalNavController.current
  val mode = settingState.library.artistMode

  val mainVM = mainViewModel
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val popupEnabled = !multiSelectState.isShowInLibrary()

  Column(
    modifier = Modifier.background(LocalTheme.current.libraryBackground)
  ) {
    ModeHeader(mode == SettingPrefs.GRID_MODE) {
      if (mode == it) {
        return@ModeHeader
      }
      settingVM.setArtistMode(if (mode == SettingPrefs.GRID_MODE) SettingPrefs.LIST_MODE else SettingPrefs.GRID_MODE)
    }

    val selectedIds by remember {
      derivedStateOf {
        multiSelectState.selectedModels(MultiSelectState.Where.Artist)
      }
    }

    if (mode == SettingPrefs.LIST_MODE) {
      val listState = rememberLazyListState()
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .verticalScrollbar(listState)
      ) {
        itemsIndexed(artists, key = { _, artist ->
          artist.artistID
        }) { _, artist ->
          ListItem(
            modifier = Modifier.height(64.dp),
            model = artist,
            text1 = artist.artist,
            selected = selectedIds.contains(artist.getKey()),
            text2 = pluralStringResource(R.plurals.song_num, artist.count, artist.count),
            popupEnabled = popupEnabled,
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
        modifier = Modifier
          .weight(1f)
          .verticalScrollbar(gridState),
        state = gridState,
        columns = GridCells.Fixed(spanCount()),
        content = {
          items(artists, key = {
            it.artistID
          }) { artist ->
            GridItem(
              artist, text1 = artist.artist,
              selected = selectedIds.contains(artist.getKey()),
              popupEnabled = popupEnabled,
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
