package remix.myplayer.compose.ui.screen.setting.logic.common

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

@Composable
fun ForceSortLogic() {
  val vm = activityViewModel<LibraryViewModel>()
  val setting = vm.settingPrefs
  var forceSort by remember { mutableStateOf(setting.forceSort) }

  SwitchPreference(
    stringResource(R.string.force_sort),
    stringResource(R.string.force_sort),
    forceSort
  ) {
    forceSort = it
    setting.forceSort = it
    vm.fetchMedia()
  }
}