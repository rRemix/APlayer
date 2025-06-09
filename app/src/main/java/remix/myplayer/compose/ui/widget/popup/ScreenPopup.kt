package remix.myplayer.compose.ui.widget.popup

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
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.LibraryViewModel

@Composable
fun ScreenPopupButton(library: Library?, vm : LibraryViewModel = activityViewModel()) {
  if (library == null) {
    return
  }
  val setting = vm.setting
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
      contentDescription = "Timer"
    )
  }

  val menuItems = library.menuItems
  val sortOrders = library.sortOrders
  val sortOrder = when (library.tag) {
    Library.TAG_SONG -> setting.songSortOrder
    Library.TAG_ALBUM -> setting.albumSortOrder
    Library.TAG_ARTIST -> setting.artistSortOrder
    Library.TAG_PLAYLIST -> setting.playlistSortOrder
    Library.TAG_GENRE -> setting.genreSortOrder
    else -> throw RuntimeException("unknown tag: ${library.tag}")
  }
  val selectedIndex = sortOrders.indexOf(sortOrder)
  if (selectedIndex < 0) {
    throw IllegalArgumentException("sortOrder:$sortOrder sortOrders: $sortOrders")
  }

  DropdownMenu(
    modifier = Modifier.wrapContentSize(),
    expanded = expanded,
    offset = DpOffset(x = 0.dp, y = -iconHeight),
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = { expanded = false }
  ) {
    fun saveSortOrder(index: Int) {
      val type = library.tag
      val ret = sortOrders[index]
      if (type == Library.TAG_SONG) {
        setting.songSortOrder = ret
      } else if (type == Library.TAG_ALBUM) {
        setting.albumSortOrder = ret
      } else if (type == Library.TAG_ARTIST) {
        setting.artistSortOrder = ret
      } else if (type == Library.TAG_PLAYLIST) {
        setting.playlistSortOrder = ret
      } else if (type == Library.TAG_GENRE) {
        setting.genreSortOrder = ret
      }
      expanded = false
      vm.fetchMedia()
    }

    menuItems.forEachIndexed { index, res ->
      DropdownMenuItem(
        text = {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(stringResource(res), color = LocalTheme.current.textPrimary)
            RadioButton(
              colors = RadioButtonDefaults.colors(selectedColor = LocalTheme.current.primary),
              selected = selectedIndex == index,
              onClick = { saveSortOrder(index) }
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