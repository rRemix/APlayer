package remix.myplayer.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.helper.SortOrder
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteCustomSort
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.settings.SettingViewModel
import remix.myplayer.viewmodel.settings.SettingsState
import remix.myplayer.viewmodel.settings.SortCategory

@Composable
fun DetailPopupButton(model: APlayerModel, onSortOrderChange: () -> Unit) {
  val nav = LocalNavController.current

  val settingVM = settingViewModel
  val settingsState by settingVM.settingsState.collectAsStateWithLifecycle()

  var expanded by remember { mutableStateOf(false) }
  var iconHeight by remember { mutableStateOf(0.dp) }

  IconButton(
    modifier = Modifier.onSizeChanged {
      iconHeight = it.height.dp
    },
    onClick = {
      expanded = !expanded
    }) {
    Icon(
      painter = painterResource(R.drawable.ic_sort_white_24dp),
      contentDescription = "DetailSortOrderPopUp"
    )
  }

  val sortOrder = model.detailSortOrder(settingsState, settingVM)
  val sortOrderItems = model.sortOrderItems()
  val selectedIndex = sortOrderItems.indexOfFirst {
    it.second == sortOrder
  }

  DropdownMenu(
    modifier = Modifier.wrapContentSize(),
    expanded = expanded,
    offset = DpOffset(x = 0.dp, y = -iconHeight),
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = { expanded = false }
  ) {

    fun saveSortOrder(index: Int) {
      if (sortOrderItems[index].first == R.string.custom) {
        // custom sort
        nav.navigate("${RouteCustomSort}/${model.getKey().toLong()}")
      } else if (model.saveDetailSortOrder(settingVM, sortOrderItems[index].second)) {
        onSortOrderChange()
      }
      expanded = false
    }

    sortOrderItems.forEachIndexed { index, item ->
      DropdownMenuItem(
        text = {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(stringResource(item.first), color = LocalTheme.current.textPrimary)
            RadioButton(
              colors = RadioButtonDefaults.colors(selectedColor = LocalTheme.current.primary),
              selected = selectedIndex == index,
              onClick = {
                saveSortOrder(index)
              }
            )
          }
        },
        onClick = {
          saveSortOrder(index)
        }
      )
    }
  }
}

private fun APlayerModel.detailSortOrder(
  settingsState: SettingsState,
  settingVM: SettingViewModel
): String {
  return when (this) {
    is Album -> settingsState.library.albumDetailSortOrder
    is Artist -> settingsState.library.artistDetailSortOrder
    is PlayList -> settingVM.settingPrefs.getPlayListDetailSortOrder(id)
    is Genre -> settingsState.library.genreDetailSortOrder
    is Folder -> settingsState.library.folderDetailSortOrder
    else -> throw Exception("unknown model: $this")
  }
}

private fun APlayerModel.saveDetailSortOrder(
  vm: SettingViewModel,
  newSortOrder: String
): Boolean {
  when (this) {
    is Album -> {
      return vm.setSortOrder(SortCategory.ALBUM_DETAIL, newSortOrder)
    }

    is Artist -> {
      return vm.setSortOrder(SortCategory.ARTIST_DETAIL, newSortOrder)
    }

    is PlayList -> {
      return vm.setSortOrder(SortCategory.PLAYLIST_DETAIL, newSortOrder, id)
    }

    is Genre -> {
      return vm.setSortOrder(SortCategory.GENRE_DETAIL, newSortOrder)
    }

    is Folder -> {
      return vm.setSortOrder(SortCategory.FOLDER_DETAIL, newSortOrder)
    }

    else -> throw Exception("unknown model: $this")
  }
}


private fun APlayerModel.sortOrderItems(): List<Pair<Int, String>> {
  val base = mutableListOf(
    Pair(R.string.title, SortOrder.SONG_A_Z),
    Pair(R.string.title_desc, SortOrder.SONG_Z_A),
    Pair(R.string.display_title, SortOrder.DISPLAY_NAME_A_Z),
    Pair(R.string.display_title_desc, SortOrder.DISPLAY_NAME_Z_A),
    Pair(R.string.album, SortOrder.ALBUM_A_Z),
    Pair(R.string.album_desc, SortOrder.ALBUM_Z_A),
    Pair(R.string.artist, SortOrder.ARTIST_A_Z),
    Pair(R.string.artist_desc, SortOrder.ARTIST_Z_A),
    Pair(R.string.date_modify, SortOrder.DATE),
    Pair(R.string.date_modify_desc, SortOrder.DATE_DESC),
  )
  when (this) {
    is Album -> {
      base.removeAt(4)
      base.removeAt(5)
      base.add(Pair(R.string.track_number, SortOrder.TRACK_NUMBER))
    }

    is Artist -> {
      base.removeAt(6)
      base.removeAt(7)
    }

    is PlayList -> {
      base.add(Pair(R.string.custom, SortOrder.PLAYLIST_SONG_CUSTOM))
    }

  }

  return base
}
