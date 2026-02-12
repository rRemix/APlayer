package remix.myplayer.ui.screen.setting

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.helper.EQHelper
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteAbout
import remix.myplayer.ui.screen.setting.logic.color.BlackThemeLogic
import remix.myplayer.ui.screen.setting.logic.color.ColoredNaviBarLogic
import remix.myplayer.ui.screen.setting.logic.color.DarkThemeLogic
import remix.myplayer.ui.screen.setting.logic.color.PrimaryColorLogic
import remix.myplayer.ui.screen.setting.logic.color.SecondaryColorLogic
import remix.myplayer.ui.screen.setting.logic.common.BlackListLogic
import remix.myplayer.ui.screen.setting.logic.common.BreakPointLogic
import remix.myplayer.ui.screen.setting.logic.common.ExportPlayListLogic
import remix.myplayer.ui.screen.setting.logic.common.ForceSortLogic
import remix.myplayer.ui.screen.setting.logic.common.ImportPlayListLogic
import remix.myplayer.ui.screen.setting.logic.common.LanguageLogic
import remix.myplayer.ui.screen.setting.logic.common.LockScreenLogic
import remix.myplayer.ui.screen.setting.logic.common.ManualScanLogic
import remix.myplayer.ui.screen.setting.logic.common.RestoreDeleteLogic
import remix.myplayer.ui.screen.setting.logic.common.ScanSizeLogic
import remix.myplayer.ui.screen.setting.logic.common.ShakeLogic
import remix.myplayer.ui.screen.setting.logic.common.ShowDisplayNameLogic
import remix.myplayer.ui.screen.setting.logic.cover.AutoDownloadLogic
import remix.myplayer.ui.screen.setting.logic.cover.DownloadSourceLogic
import remix.myplayer.ui.screen.setting.logic.cover.IgnoreMediaStoreLogic
import remix.myplayer.ui.screen.setting.logic.library.LibraryLogic
import remix.myplayer.ui.screen.setting.logic.lyric.DesktopLyricLogic
import remix.myplayer.ui.screen.setting.logic.lyric.LyricPriorityLogic
import remix.myplayer.ui.screen.setting.logic.lyric.StatusBarLyricLogic
import remix.myplayer.ui.screen.setting.logic.notification.ClassicNotifyLogic
import remix.myplayer.ui.screen.setting.logic.notification.NotifyBackgroundLogic
import remix.myplayer.ui.screen.setting.logic.other.ClearCacheLogic
import remix.myplayer.ui.screen.setting.logic.other.FeedbackLogic
import remix.myplayer.ui.screen.setting.logic.play.AutoPlayLogic
import remix.myplayer.ui.screen.setting.logic.play.IgnoreAudioFocusLogic
import remix.myplayer.ui.screen.setting.logic.play.PlayFadeLogic
import remix.myplayer.ui.screen.setting.logic.playingscreen.KeepScreenOnLogic
import remix.myplayer.ui.screen.setting.logic.playingscreen.PlayingCoverAnimationLogic
import remix.myplayer.ui.screen.setting.logic.playingscreen.PlayingScreenBackgroundLogic
import remix.myplayer.ui.screen.setting.logic.playingscreen.PlayingScreenBottomLogic
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.viewmodel.mainViewModel

@Composable
fun SettingScreen() {
  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.setting), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->

    LazyColumn(
      modifier = Modifier
        .padding(contentPadding)
    ) {
      item {
        CommonPreferences()

        PlayPreferences()

        ColorPreferences()

        LibraryPreferences()

        PlayingScreenPreferences()

        CoverPreferences()

        LyricPreferences()

        NotificationPreferences()

        OtherPreferences()
      }
    }
  }
}

@Composable
private fun CommonPreferences() {
  SettingTitle(R.string.common)

  ScanSizeLogic()

  BlackListLogic()

  LockScreenLogic()

  ManualScanLogic()

  ImportPlayListLogic()

  ExportPlayListLogic()

  RestoreDeleteLogic()

  LanguageLogic()

  ShakeLogic()

  ShowDisplayNameLogic()

  ForceSortLogic()
}

@Composable
private fun PlayPreferences() {
  SettingTitle(R.string.play)

  IgnoreAudioFocusLogic()

  BreakPointLogic()

  PlayFadeLogic()

  AutoPlayLogic()
}

@Composable
private fun ColorPreferences() {
  SettingTitle(R.string.color)

  DarkThemeLogic()

  BlackThemeLogic()

  PrimaryColorLogic()

  SecondaryColorLogic()

  ColoredNaviBarLogic()

}

@Composable
private fun LibraryPreferences() {
  SettingTitle(R.string.library)

  LibraryLogic()
}

@Composable
private fun PlayingScreenPreferences() {
  SettingTitle(R.string.playing_screen)

  PlayingScreenBackgroundLogic()

  PlayingScreenBottomLogic()

  KeepScreenOnLogic()
}

@Composable
private fun CoverPreferences() {
  SettingTitle(R.string.cover)

  PlayingCoverAnimationLogic()

  IgnoreMediaStoreLogic()

  AutoDownloadLogic()

  DownloadSourceLogic()
}

@Composable
private fun NotificationPreferences() {
  SettingTitle(R.string.notify)

  ClassicNotifyLogic()

  NotifyBackgroundLogic()
}

@Composable
private fun LyricPreferences() {
  SettingTitle(R.string.lrc)

  DesktopLyricLogic()

  StatusBarLyricLogic()

  LyricPriorityLogic()
}

@Composable
private fun OtherPreferences() {
  SettingTitle(R.string.other)

  val mainViewModel = mainViewModel
  val activity = LocalActivity.current
  val nav = LocalNavController.current

  ArrowPreference(R.string.eq_setting) {
    EQHelper.startEqualizer(activity ?: return@ArrowPreference, nav)
  }

  FeedbackLogic()

  ArrowPreference(R.string.about_info) {
    nav.navigate(RouteAbout)
  }

  if (BuildConfig.FLAVOR == "normal") {
    Preference(onClick = {
      mainViewModel.checkInAppUpdate(true)
    }, title = stringResource(R.string.check_update))
  }

  ClearCacheLogic()
}

@Composable
private fun SettingTitle(res: Int) {
  Text(
    modifier = Modifier.padding(14.dp),
    text = stringResource(res),
    fontSize = 16.sp,
    color = LocalTheme.current.secondary
  )
}
