package remix.myplayer.compose.ui.screen.setting.logic.playingscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.settingViewModel

private val itemRes = listOf(R.string.now_playing_screen_theme, R.string.now_playing_screen_cover)

@Composable
fun PlayingScreenBackgroundLogic() {
  val vm = settingViewModel
  val context = LocalContext.current
  var background by remember {
    mutableIntStateOf(vm.settingPrefs.playingScreenBackground)
  }

  val state = rememberDialogState(false)
  val content by remember {
    derivedStateOf {
      context.getString(itemRes[background])
    }
  }

  NormalPreference(
    stringResource(R.string.now_playing_screen_background),
    content
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
      background = index
      vm.settingPrefs.playingScreenBackground = index
    }
  )
}