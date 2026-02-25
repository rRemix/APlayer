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
fun PrimaryColorLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val state = rememberDialogState(false)

  ThemePreference(
    stringResource(R.string.primary_color),
    stringResource(R.string.primary_color_tip)
  ) {
    state.show()
  }

  var primaryColor by remember(state.isOpen) {
    mutableStateOf(settingState.color.primaryColor)
  }

  ColorDialog(
    dialogState = state,
    titleRes = R.string.primary_color,
    initialColor = primaryColor,
    onColorChange = {
      primaryColor = it
    },
    onPositive = {
      settingVM.setPrimaryColor(primaryColor)
    }
  )
}