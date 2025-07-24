package remix.myplayer.compose.ui.screen.setting.logic.lyric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.service.Command
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util.isSupportStatusBarLyric
import remix.myplayer.util.Util.sendLocalBroadcast

@Composable
fun StatusBarLyricLogic() {
  val context = LocalContext.current
  if (!isSupportStatusBarLyric(context)) {
    return
  }
  val vm = settingViewModel
  var statusBarLyric by remember { mutableStateOf(vm.settingPrefs.statusBarLyric) }

  SwitchPreference(
    stringResource(R.string.statusbar_lrc),
    checked = statusBarLyric
  ) {
    statusBarLyric = it
    vm.settingPrefs.statusBarLyric = it

    val intent =
      MusicUtil.makeCmdIntent(Command.TOGGLE_STATUS_BAR_LRC)
    sendLocalBroadcast(intent)
  }
}