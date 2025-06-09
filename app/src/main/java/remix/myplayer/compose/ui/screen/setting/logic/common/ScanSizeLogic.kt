package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.util.Constants.KB
import remix.myplayer.util.Constants.MB

private val scanSize = intArrayOf(0, 500 * KB, MB, 2 * MB, 5 * MB)

@Composable
fun ScanSizeLogic() {
  val vm: LibraryViewModel = activityViewModel()
  val setting = vm.setting

  var scanSizeState = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.music_filter),
    stringResource(R.string.set_filter_size)
  ) {
    scanSizeState.show()
  }

  var position by rememberSaveable {
    mutableIntStateOf(scanSize.indexOfFirst {
      it == setting.scanSize
    })
  }
  if (position < 0) {
    throw IllegalArgumentException("illegal pos, scanSize: ${setting.scanSize}")
  }

  NormalDialog(
    dialogState = scanSizeState,
    title = stringResource(R.string.set_filter_size),
    positive = null,
    negative = null,
    items = listOf("0K", "500K", "1MB", "2MB", "5MB"),
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(position) { index ->
      position = index
      setting.scanSize = scanSize[index]
      vm.fetchMedia()
    }
  )
}