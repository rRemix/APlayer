package remix.myplayer.ui.screen.setting.logic.lyric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.service.Command
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util.isSupportStatusBarLyric
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun StatusBarLyricLogic() {
  val context = LocalContext.current
  if (!isSupportStatusBarLyric(context)) {
    return
  }

  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  SwitchPreference(
    stringResource(R.string.statusbar_lrc),
    checked = settingState.lyric.statusBarLyricEnabled
  ) {
    settingVM.setStatusBarLyricEnabled(it)

    val intent =
      MusicUtil.makeCmdIntent(Command.TOGGLE_STATUS_BAR_LRC)
    sendLocalBroadcast(intent)
  }
}