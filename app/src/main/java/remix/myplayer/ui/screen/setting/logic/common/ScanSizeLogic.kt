package remix.myplayer.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.util.Constants.KB
import remix.myplayer.util.Constants.MB
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel

private val items = intArrayOf(0, 500 * KB, MB, 2 * MB, 5 * MB)

@Composable
fun ScanSizeLogic() {
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val scanSizeState = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.music_filter),
    stringResource(R.string.set_filter_size)
  ) {
    scanSizeState.show()
  }

  val select = items.indexOfFirst {
    it == settingState.common.scanSize
  }
  if (select < 0) {
    throw IllegalArgumentException("illegal pos, scanSize: ${settingState.common.scanSize}")
  }

  NormalDialog(
    dialogState = scanSizeState,
    title = stringResource(R.string.set_filter_size),
    positive = null,
    negative = null,
    items = listOf("0K", "500K", "1MB", "2MB", "5MB"),
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(select) {
      if (select == it) {
        return@ItemsCallbackSingleChoice
      }

      settingVM.setScanSize(items[it])
      libraryVM.fetchMedia()
    }
  )
}