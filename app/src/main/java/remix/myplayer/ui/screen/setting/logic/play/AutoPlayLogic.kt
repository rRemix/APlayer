package remix.myplayer.ui.screen.setting.logic.play

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
fun AutoPlayLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val autoPlay = settingState.play.autoPlay
  val state = rememberDialogState(false)

  NormalPreference(stringResource(R.string.auto_play), stringResource(R.string.auto_play_tip)) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.auto_play,
    itemRes = listOf(
      R.string.auto_play_headset_plug,
      R.string.auto_play_open_software,
      R.string.auto_play_none
    ),
    positiveRes = null,
    negativeRes = null,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(autoPlay) {
      if (autoPlay == it) {
        return@ItemsCallbackSingleChoice
      }
      settingVM.setAutoPlay(it)
    }
  )

}