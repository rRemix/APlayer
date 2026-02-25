package remix.myplayer.ui.screen.setting.logic.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.dialog.ColorDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.ThemePreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun SecondaryColorLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val state = rememberDialogState(false)

  ThemePreference(
    stringResource(R.string.accent_color),
    stringResource(R.string.accent_color_tip),
    false
  ) {
    state.show()
  }

  var secondaryColor by remember(state.isOpen) {
    mutableStateOf(settingState.color.secondaryColor)
  }

  ColorDialog(
    dialogState = state,
    titleRes = R.string.accent_color,
    initialColor = secondaryColor,
    onColorChange = {
      secondaryColor = it
    },
    onPositive = {
      settingVM.setSecondaryColor(secondaryColor)
    }
  )
}