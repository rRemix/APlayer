package remix.myplayer.ui.screen.setting.logic.lyric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun TranslationLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  SwitchPreference(
    stringResource(R.string.lyric_translation_title),
    stringResource(R.string.lyric_translation_tip),
    settingState.lyric.translationEnabled
  ) {
    settingVM.setLyricTranslationEnabled(it)
  }
}
