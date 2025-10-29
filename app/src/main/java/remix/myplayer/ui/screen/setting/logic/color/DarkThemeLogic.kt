package remix.myplayer.ui.screen.setting.logic.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.ThemePrefs
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel

private val itemRes = listOf(
  R.string.always_off,
  R.string.always_on,
  R.string.follow_system
)

@Composable
fun DarkThemeLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val state = rememberDialogState(false)

  val selected = when (settingState.color.darkTheme) {
    ThemePrefs.ALWAYS_OFF -> 0
    ThemePrefs.ALWAYS_ON -> 1
    else -> 2
  }

  NormalPreference(stringResource(R.string.dark_theme), stringResource(itemRes[selected])) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.dark_theme,
    itemRes = itemRes,
    positiveRes = null,
    negativeRes = null,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(selected) {
      if (selected == it) {
        return@ItemsCallbackSingleChoice
      }

      settingVM.setDarkTheme(
        when (it) {
          0 -> ThemePrefs.ALWAYS_OFF
          1 -> ThemePrefs.ALWAYS_ON
          else -> ThemePrefs.FOLLOW_SYSTEM
        }
      )
    }
  )
}