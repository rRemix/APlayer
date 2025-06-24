package remix.myplayer.compose.ui.screen.setting.logic.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel

private val itemRes = listOf(R.string.lastfm, R.string.netease)

@Composable
fun DownloadSourceLogic() {
  val settingVM = activityViewModel<SettingViewModel>()
  val libraryVM = activityViewModel<LibraryViewModel>()
  val context = LocalContext.current
  var selected by remember {
    mutableIntStateOf(settingVM.settingPrefs.downloadSource)
  }

  val content by remember {
    derivedStateOf {
      context.getString(if (selected == 0) R.string.cover_download_from_lastfm else R.string.cover_download_from_netease)
    }
  }
  val state = rememberDialogState(false)

  NormalPreference(
    stringResource(R.string.cover_download_source),
    content
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
      selected = it
      settingVM.settingPrefs.downloadSource = it

      libraryVM.fetchMedia(true)
    }
  )
}