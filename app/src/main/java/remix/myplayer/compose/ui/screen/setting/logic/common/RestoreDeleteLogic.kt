package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.util.ToastUtil

@Composable
fun RestoreDeleteLogic() {
  val context = LocalContext.current
  val vm = activityViewModel<LibraryViewModel>()
  val setting = vm.setting

  NormalPreference(
    stringResource(R.string.restore_songs),
    stringResource(R.string.restore_songs_tip)
  ) {
    vm.fetchMedia()
    setting.deleteIds = emptySet<String>()
    ToastUtil.show(context, R.string.alread_restore_songs)
  }

}