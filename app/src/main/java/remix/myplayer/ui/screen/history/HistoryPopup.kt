package remix.myplayer.ui.screen.history

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
import remix.myplayer.helper.SortOrder
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.libraryViewModel

@Composable
fun HistoryPopup() {
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
      contentDescription = "HistoryOrderPopUp"
    )
  }

  val sortOrder = settingPrefs.historySortOrder
  val sortOrderItems = listOf(
    Pair(R.string.count, SortOrder.PLAY_COUNT),
    Pair(R.string.count_desc, SortOrder.PLAY_COUNT_DESC),
    Pair(R.string.last_play, SortOrder.LAST_PLAY),
    Pair(R.string.last_play_desc, SortOrder.LAST_PLAY_DESC),

    )
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
      settingPrefs.historySortOrder = sortOrderItems[index].second
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