package remix.myplayer.ui.screen.setting.logic.playingscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun PlayingScreenBottomLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val bottom = settingState.playingScreen.bottom

  val state = rememberDialogState(false)

  NormalPreference(
    stringResource(R.string.show_on_bottom),
    stringResource(R.string.show_of_bottom_tip)
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.show_on_bottom,
    itemRes = listOf(
      R.string.show_next_song_only,
      R.string.show_vol_control_only,
      R.string.tap_to_toggle,
      R.string.close
    ),
    positiveRes = null,
    negativeRes = null,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(bottom) {
      if (bottom == it) {
        return@ItemsCallbackSingleChoice
      }
      settingVM.setPlayingScreenBottom(it)
    }
  )
}