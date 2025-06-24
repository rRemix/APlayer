package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel

@Composable
fun ShowDisplayNameLogic() {
  val vm = activityViewModel<LibraryViewModel>()
  val setting = vm.settingPrefs
  var displayName by remember { mutableStateOf(setting.showDisplayName) }

  SwitchPreference(
    stringResource(R.string.show_displayname),
    stringResource(R.string.show_displayname_tip),
    displayName
  ) {
    Song.SHOW_DISPLAYNAME = it
    displayName = it
    setting.showDisplayName = it
    vm.fetchMedia()
  }
}