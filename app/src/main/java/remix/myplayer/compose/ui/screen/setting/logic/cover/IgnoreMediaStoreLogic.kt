package remix.myplayer.compose.ui.screen.setting.logic.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel

@Composable
fun IgnoreMediaStoreLogic() {
  val settingVM = activityViewModel<SettingViewModel>()
  val libraryVM = activityViewModel<LibraryViewModel>()
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