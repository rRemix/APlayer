package remix.myplayer.ui.screen.setting.logic.lyric

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.Util
import remix.myplayer.util.ext.ShowLyricTipDialog
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.settings.SettingViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LyricPriorityLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val dialogState = rememberDialogState()

  LyricTip(settingVM, dialogState)

  var orderList by remember {
    mutableStateOf(settingState.lyric.generalLyricOrder)
  }
  NormalDialog(
    dialogState = dialogState,
    titleRes = R.string.lrc_priority,
    onDismissRequest = {
      orderList = settingState.lyric.generalLyricOrder
    },
    onPositive = {
      try {
        settingVM.setGeneralLyricOrder(orderList)

        MessageNotifier.show(R.string.save_success)
      } catch (e: Exception) {
        MessageNotifier.show(R.string.save_error_arg, e.message.toString())
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
        items(orderList, key = { it }) { lyricOrder ->
          ReorderableItem(reorderableLazyListState, key = lyricOrder) { isDragging ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = stringResource(lyricOrder.stringRes),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
              )
              Icon(
                painter = painterResource(R.drawable.ic_drag_handle_24dp),
                contentDescription = "LyricDragHandle",
                tint = LocalTheme.current.textSecondary,
                modifier = Modifier
                  .align(Alignment.CenterEnd)
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

@Composable
private fun LyricTip(
  settingVM: SettingViewModel,
  dialogState: DialogState
) {
  var showLyricTip by rememberSaveable { mutableStateOf(false) }
  if (showLyricTip) {
    settingVM.lyricPrefs.tipShown = true
    ShowLyricTipDialog {
      settingVM.lyricPrefs.tipShown = true
      dialogState.show()
    }
  }
  NormalPreference(
    stringResource(R.string.lrc_priority),
    stringResource(R.string.lrc_priority_tip)
  ) {
    if (settingVM.lyricPrefs.tipShown) {
      dialogState.show()
    } else {
      showLyricTip = true
    }
  }
}
