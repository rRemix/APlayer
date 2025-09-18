package remix.myplayer.compose.ui.screen.setting.logic.lyric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.EntryPointAccessors
import remix.myplayer.R
import remix.myplayer.compose.lyric.LyricsManagerEntryPoint
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
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

  val lyricsManager = remember {
    EntryPointAccessors.fromApplication(
      context.applicationContext,
      LyricsManagerEntryPoint::class.java
    ).lyricsManager()
  }

  var statusBarLyric by remember { mutableStateOf(lyricsManager.isStatusBarLyricEnabled) }

  SwitchPreference(
    stringResource(R.string.statusbar_lrc),
    checked = statusBarLyric
  ) {
    statusBarLyric = it
    lyricsManager.isStatusBarLyricEnabled = it

    val intent =
      MusicUtil.makeCmdIntent(Command.TOGGLE_STATUS_BAR_LRC)
    sendLocalBroadcast(intent)
  }
}