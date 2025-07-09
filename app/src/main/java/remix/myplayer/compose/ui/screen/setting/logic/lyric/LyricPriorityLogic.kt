package remix.myplayer.compose.ui.screen.setting.logic.lyric

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LyricPriorityLogic() {
  val vm = activityViewModel<SettingViewModel>()
  val context = LocalContext.current
  val hapticFeedback = LocalHapticFeedback.current
  val dialogState = rememberDialogState()

  NormalPreference(
    stringResource(R.string.lrc_priority),
    stringResource(R.string.lrc_priority_tip)
  ) {
    dialogState.show()
  }

  var orderList by remember {
    mutableStateOf(vm.lyricPrefs.lyricOrderList)
  }
  NormalDialog(
    dialogState = dialogState,
    titleRes = R.string.lrc_priority,
    onDismissRequest = {
      orderList = vm.lyricPrefs.lyricOrderList
    },
    onPositive = {
      try {
        DiskCache.getLrcDiskCache().delete()
        DiskCache.init(context, "lyric")
        SPUtil.deleteFile(context, SPUtil.LYRIC_KEY.NAME)
        vm.lyricPrefs.lyricOrder = Gson().toJson(orderList)
        ToastUtil.show(context, R.string.save_success)
      } catch (e: Exception) {
        ToastUtil.show(context, R.string.save_error_arg, e.message)
      }
    },
    custom = {
      val lazyListState = rememberLazyListState()
      val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderList = orderList.toMutableList().apply {
          add(to.index, removeAt(from.index))
        }

        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
      }

      LazyColumn(
        state = lazyListState,
      ) {
        items(orderList, key = { it }) { lyricOrder ->
          ReorderableItem(reorderableLazyListState, key = lyricOrder) { isDragging ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .draggableHandle(
                  onDragStarted = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                  },
                  onDragStopped = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                  },
                ),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = stringResource(lyricOrder.stringRes),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  )
}