package remix.myplayer.ui.screen.setting.logic.playingscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel

private val itemRes = listOf(R.string.now_playing_screen_theme, R.string.now_playing_screen_cover)

@Composable
fun PlayingScreenBackgroundLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val background = settingState.playingScreen.background

  val state = rememberDialogState(false)

  NormalPreference(
    stringResource(R.string.now_playing_screen_background),
    stringResource(itemRes[background])
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.now_playing_screen_background,
    itemRes = itemRes,
    positiveRes = null,
    negativeRes = null,
    itemsCallback = { index, _ ->
      if (background == index) {
        return@NormalDialog
      }
      settingVM.setPlayingScreenBackground(index)
    }
  )
}