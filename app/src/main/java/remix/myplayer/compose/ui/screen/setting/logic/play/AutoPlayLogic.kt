package remix.myplayer.compose.ui.screen.setting.logic.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel

@Composable
fun AutoPlayLogic() {
    val state = rememberDialogState(false)
    val setting = activityViewModel<LibraryViewModel>().settingPrefs
    var autoPlay by remember {
        mutableIntStateOf(setting.autoPlay)
    }

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
            setting.autoPlay = it
            autoPlay = it
        }
    )

}