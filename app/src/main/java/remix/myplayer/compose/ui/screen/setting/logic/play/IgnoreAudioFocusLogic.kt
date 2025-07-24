package remix.myplayer.compose.ui.screen.setting.logic.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.libraryViewModel

@Composable
fun IgnoreAudioFocusLogic() {
    val setting = libraryViewModel.settingPrefs

    var ignoreAudioFocus by remember { mutableStateOf(setting.ignoreAudioFocus) }

    SwitchPreference(
        stringResource(R.string.audio_focus),
        stringResource(R.string.audio_focus_tip),
        ignoreAudioFocus
    ) {
        ignoreAudioFocus = it
        setting.ignoreAudioFocus = it
    }
}