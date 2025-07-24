package remix.myplayer.compose.ui.screen.setting.logic.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
fun IgnoreMediaStoreLogic() {
  val settingVM = settingViewModel
  val libraryVM = libraryViewModel
  var ignoreMediaStore by remember { mutableStateOf(settingVM.settingPrefs.ignoreMediaStore) }

  SwitchPreference(
    stringResource(R.string.ignore_mediastore_artwork),
    stringResource(R.string.ignore_mediastore_artwork_tips),
    ignoreMediaStore
  ) {
    ignoreMediaStore = it
    settingVM.settingPrefs.ignoreMediaStore = it
    libraryVM.fetchMedia(true)
  }
}