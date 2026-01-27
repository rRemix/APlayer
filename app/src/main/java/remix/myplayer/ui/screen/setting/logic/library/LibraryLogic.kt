package remix.myplayer.ui.screen.setting.logic.library

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.util.Util
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.settings.SettingViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LibraryLogic() {
  val vm: SettingViewModel = settingViewModel

  val currentLibrary by vm.currentLibrary.collectAsStateWithLifecycle()
  val libraryConfig by vm.libraryConfig.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var orderList by remember {
    mutableStateOf(libraryConfig)
  }

  fun resetState() {
    orderList = libraryConfig
  }

  val state = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.library_category),
    stringResource(R.string.configure_library_category)
  ) {
    resetState()
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.library_category,
    onDismissRequest = {
      resetState()
    },
    onPositive = {
      if (orderList.none { it.enable }) {
        return@NormalDialog
      }
      if (libraryConfig == orderList) {
        return@NormalDialog
      }

      vm.updateAllLibraries(orderList)

      if (orderList.none { it.tag == currentLibrary.tag && it.enable }) {
        vm.changeLibrary(orderList.first { it.enable })
      }
    },
    custom = {
      val lazyListState = rememberLazyListState()
      val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderList = orderList.toMutableList().apply {
          add(to.index, removeAt(from.index))
        }

        Util.vibrate(context, 50)
      }

      LazyColumn(
        state = lazyListState,
      ) {
        items(orderList, key = { it.tag }) { library ->
          ReorderableItem(reorderableLazyListState, key = library.tag) { isDragging ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickWithRipple(false) {
                  orderList = orderList.map {
                    if (it.tag == library.tag) it.copy(enable = !it.enable) else it
                  }
                },
              verticalAlignment = Alignment.CenterVertically
            ) {
              CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(
                  modifier = Modifier.padding(end = 8.dp),
                  checked = library.enable,
                  onCheckedChange = { checked ->
                    orderList = orderList.map {
                      if (it.tag == library.tag) it.copy(enable = checked) else it
                    }
                  })
              }
              TextPrimary(
                text = stringResource(library.stringRes),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
              )
              Icon(
                painter = painterResource(R.drawable.ic_drag_handle_24dp),
                contentDescription = "LibraryDragHandle",
                tint = LocalTheme.current.textSecondary,
                modifier = Modifier
                  .padding(end = 4.dp)
                  .draggableHandle(
                    onDragStarted = {
                      Util.vibrate(context, 50)
                    },
                    onDragStopped = {
                      Util.vibrate(context, 50)
                    },
                  )
              )
            }
          }
        }
      }
    }
  )
}
