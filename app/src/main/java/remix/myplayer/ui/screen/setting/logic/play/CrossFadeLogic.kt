package remix.myplayer.ui.screen.setting.logic.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun PlayFadeLogic() {
    val settingVM = settingViewModel
    val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

    SwitchPreference(
        stringResource(R.string.play_cross_fade),
        stringResource(R.string.play_cross_fade_tip),
        settingState.play.crossFade
    ) {
        settingVM.setCrossFade(it)
    }
}