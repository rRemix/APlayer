package remix.myplayer.ui.screen.setting.logic.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun SourceOpenActionLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val action = settingState.play.sourceOpenAction
  val dialog = rememberDialogState(false)

  val selectedLabel = when (action) {
    SettingPrefs.SOURCE_OPEN_ASK -> R.string.source_open_ask
    SettingPrefs.SOURCE_OPEN_RESUME -> R.string.source_open_resume
    else -> R.string.source_open_only
  }

  NormalPreference(
    stringResource(R.string.source_open_action),
    stringResource(selectedLabel)
  ) {
    dialog.show()
  }

  NormalDialog(
    dialogState = dialog,
    titleRes = R.string.source_open_action,
    itemRes = listOf(
      R.string.source_open_only,
      R.string.source_open_ask,
      R.string.source_open_resume
    ),
    positiveRes = null,
    negativeRes = null,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(action) { selected ->
      if (action != selected) settingVM.setSourceOpenAction(selected)
    }
  )
}
