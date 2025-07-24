package remix.myplayer.compose.ui.screen.setting.logic.playingscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
fun PlayingScreenBottomLogic() {
  val vm = settingViewModel
  var bottom by remember {
    mutableIntStateOf(vm.settingPrefs.playingScreenBottom)
  }

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
      bottom = it
      vm.settingPrefs.playingScreenBottom = it
    }
  )
}