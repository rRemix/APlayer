package remix.myplayer.compose.ui.screen.setting.logic.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel

@Composable
fun PlayFadeLogic() {
    val setting = activityViewModel<LibraryViewModel>().settingPrefs

    var playFade by remember { mutableStateOf(setting.playFade) }
    SwitchPreference(
        stringResource(R.string.play_cross_fade),
        stringResource(R.string.play_cross_fade_tip),
        playFade
    ) {
        playFade = it
        setting.playFade = it
    }
}