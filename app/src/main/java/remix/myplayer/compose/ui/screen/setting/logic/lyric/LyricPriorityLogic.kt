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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import remix.myplayer.R
import remix.myplayer.compose.lyric.LyricManagerEntryPoint
import remix.myplayer.compose.nav.UiMessageDispatcher
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.ui.ViewCommon.ShowLyricTipDialog
import remix.myplayer.util.SPUtil
import remix.myplayer.util.Util
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LyricPriorityLogic() {
  val vm = settingViewModel
  val context = LocalContext.current
  val lyricsManager = remember {
    EntryPointAccessors.fromApplication(
      context,
      LyricManagerEntryPoint::class.java
    ).lyricManager()
  }

  val dialogState = rememberDialogState()

  var showLyricTip by rememberSaveable { mutableStateOf(false) }
  if (showLyricTip) {
    vm.lyricPrefs.tipShown = true
    ShowLyricTipDialog {
      vm.lyricPrefs.tipShown = true
      dialogState.show()
    }
  }
  NormalPreference(
    stringResource(R.string.lrc_priority),
    stringResource(R.string.lrc_priority_tip)
  ) {
    if (vm.lyricPrefs.tipShown) {
      dialogState.show()
    } else {
      showLyricTip = true
    }
  }

  var orderList by remember {
    mutableStateOf(vm.lyricPrefs.generalLyricOrderList)
  }
  NormalDialog(
    dialogState = dialogState,
    titleRes = R.string.lrc_priority,
    onDismissRequest = {
      orderList = vm.lyricPrefs.generalLyricOrderList
    },
    onPositive = {
      try {
        // TODO 和LyricSearch统一缓存目录
        DiskCache.getLrcDiskCache().delete()
        DiskCache.init(context, "lyric")
//        lyricsManager.clearAllCache()
        vm.lyricPrefs.clearUserSave()
        vm.lyricPrefs.generalLyricOrder = Gson().toJson(orderList)
        UiMessageDispatcher.show(R.string.save_success)
      } catch (e: Exception) {
        UiMessageDispatcher.show(R.string.save_error_arg, e.message.toString())
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
                .height(48.dp)
                .draggableHandle(
                  onDragStarted = {
                    Util.vibrate(context, 50)
                  },
                  onDragStopped = {
                    Util.vibrate(context, 50)
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