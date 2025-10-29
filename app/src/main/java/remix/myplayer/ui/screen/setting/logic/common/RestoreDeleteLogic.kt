package remix.myplayer.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun RestoreDeleteLogic() {
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel

  NormalPreference(
    stringResource(R.string.restore_songs),
    stringResource(R.string.restore_songs_tip)
  ) {
    settingVM.setDeleteIds(emptySet())
    libraryVM.fetchMedia()
    MessageNotifier.show(R.string.alread_restore_songs)
  }

}