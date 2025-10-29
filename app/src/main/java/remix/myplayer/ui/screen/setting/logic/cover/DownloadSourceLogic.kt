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

private val itemRes = listOf(R.string.lastfm, R.string.netease)

@Composable
fun DownloadSourceLogic() {
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  var selected = settingState.cover.downloadSource

  val state = rememberDialogState(false)

  NormalPreference(
    stringResource(R.string.cover_download_source),
    stringResource(itemRes[selected])
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.cover_download_source,
    positiveRes = null,
    negativeRes = null,
    itemRes = itemRes,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(selected) {
      if (selected == it) {
        return@ItemsCallbackSingleChoice
      }
      settingVM.setDownloadSource(it)
      libraryVM.fetchMedia(true)
    }
  )
}