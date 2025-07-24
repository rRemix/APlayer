package remix.myplayer.compose.ui.screen.detail

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
import remix.myplayer.R
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.nav.RouteCustomSort
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.SortOrder

@Composable
fun DetailPopupButton(model: APlayerModel, onSortOrderChange: () -> Unit) {
  val nav = LocalNavController.current
  val libraryVM = libraryViewModel
  val settingPrefs = libraryVM.settingPrefs

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

  val sortOrder = model.detailSortOrder(settingPrefs)
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
      } else if (model.saveDetailSortOrder(settingPrefs, sortOrderItems[index].second)) {
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

private fun APlayerModel.detailSortOrder(settingPrefs: SettingPrefs): String {
  return when (this) {
    is Album -> settingPrefs.albumDetailSortOrder
    is Artist -> settingPrefs.artistDetailSortOrder
    is PlayList -> settingPrefs.playListDetailSortOrder
    is Genre -> settingPrefs.genreDetailSortOrder
    is Folder -> settingPrefs.folderDetailSortOrder
    else -> throw Exception("unknown model: $this")
  }
}

private fun APlayerModel.saveDetailSortOrder(
  settingPrefs: SettingPrefs,
  newSortOrder: String
): Boolean {
  when (this) {
    is Album -> {
      if (settingPrefs.albumDetailSortOrder != newSortOrder) {
        settingPrefs.albumDetailSortOrder = newSortOrder
        return true
      } else {
        return false
      }
    }

    is Artist -> {
      if (settingPrefs.artistDetailSortOrder != newSortOrder) {
        settingPrefs.artistDetailSortOrder = newSortOrder
        return true
      } else {
        return false
      }
    }

    is PlayList -> {
      if (settingPrefs.playListDetailSortOrder != newSortOrder) {
        settingPrefs.playListDetailSortOrder = newSortOrder
        return true
      } else {
        return false
      }
    }

    is Genre -> {
      if (settingPrefs.genreDetailSortOrder != newSortOrder) {
        settingPrefs.genreDetailSortOrder = newSortOrder
        return true
      } else {
        return false
      }
    }

    is Folder -> {
      if (settingPrefs.folderDetailSortOrder != newSortOrder) {
        settingPrefs.folderDetailSortOrder = newSortOrder
        return true
      } else {
        return false
      }
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