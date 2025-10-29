package remix.myplayer.ui.screen.setting.logic.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel

private val itemRes = listOf(R.string.always, R.string.wifi_only, R.string.never)

@Composable
fun AutoDownloadLogic() {
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val selected = settingState.cover.autoDownloadCover

  val state = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.auto_download_album_artist_cover),
    stringResource(itemRes[selected])
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.auto_download_album_artist_cover,
    positiveRes = null,
    negativeRes = null,
    itemRes = itemRes,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(selected) {
      if (selected == it) {
        return@ItemsCallbackSingleChoice
      }
      settingVM.setAutoDownloadCover(it)
      libraryVM.fetchMedia(true)
    }
  )
}