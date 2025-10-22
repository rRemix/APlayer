package remix.myplayer.compose.ui.screen.setting.logic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.prefs.ThemePrefs
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.ui.theme.LocalThemeController

private val itemRes = listOf(
  R.string.always_off,
  R.string.always_on,
  R.string.follow_system
)

@Composable
fun DarkThemeLogic() {
  val state = rememberDialogState(false)
  val controller = LocalThemeController.current

  var selected by remember {
    mutableIntStateOf(
      when (controller.dark) {
        ThemePrefs.ALWAYS_OFF -> 0
        ThemePrefs.ALWAYS_ON -> 1
        else -> 2
      }
    )
  }

  var content by remember {
    mutableIntStateOf(itemRes[selected])
  }

  NormalPreference(stringResource(R.string.dark_theme), stringResource(content)) {
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
      content = itemRes[it]
      controller.dark = when (it) {
        0 -> ThemePrefs.ALWAYS_OFF
        1 -> ThemePrefs.ALWAYS_ON
        else -> ThemePrefs.FOLLOW_SYSTEM
      }
      selected = it
    }
  )
}