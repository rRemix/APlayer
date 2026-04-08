package remix.myplayer.ui.screen.setting.logic.lyric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hchen.superlyricapi.SuperLyricHelper
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun SuperLyricApiLogic() {
  if (!SuperLyricHelper.isAvailable()) {
    return
  }

  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  SwitchPreference(
    title = "SuperLyricApi",
    content = "API version: ${SuperLyricHelper.getApiVersion()}",
    checked = settingState.lyric.superLyricApiEnabled
  ) {
    settingVM.setSuperLyricApiEnabled(it)
  }
}