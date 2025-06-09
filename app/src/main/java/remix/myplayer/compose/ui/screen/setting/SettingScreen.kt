package remix.myplayer.compose.ui.screen.setting

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.compose.ui.screen.setting.logic.common.BlackListLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.BreakPointLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ExportPlayListLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ForceSortLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ImportPlayListLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.LanguageLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.LockScreenLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ManualScanLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.RestoreDeleteLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ScanSizeLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ShakeLogic
import remix.myplayer.compose.ui.screen.setting.logic.common.ShowDisplayNameLogic
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.CommonAppBar
import remix.myplayer.compose.ui.widget.common.TextSecondary

private const val REQUEST_THEME_COLOR = 0x10
private const val REQUEST_IMPORT_PLAYLIST = 0x102
private const val REQUEST_EXPORT_PLAYLIST = 0x103
private const val REQUEST_CODE_ADD_BLACKLIST = 0x104

@Composable
fun SettingScreen() {
  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.setting)) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->

    LazyColumn(
      modifier = Modifier
        .padding(contentPadding)
    ) {

      item {
        CommonPreferences()

        SettingTitle(R.string.play)
        var ignoreFocus by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.audio_focus),
          stringResource(R.string.audio_focus_tip),
          ignoreFocus
        ) {
          ignoreFocus = !ignoreFocus
        }

        var playFade by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.play_cross_fade),
          stringResource(R.string.play_cross_fade_tip),
          playFade
        ) {

        }

        NormalPreference(
          stringResource(R.string.auto_play),
          stringResource(R.string.audio_focus_tip)
        ) {

        }

        SettingTitle(R.string.color)

        NormalPreference(stringResource(R.string.dark_theme)) {

        }

        var blackTheme by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.black_theme),
          stringResource(R.string.black_theme_tip),
          blackTheme
        ) {

        }

        ThemePreference(
          stringResource(R.string.primary_color),
          stringResource(R.string.primary_color_tip)
        ) {

        }

        ThemePreference(
          stringResource(R.string.accent_color),
          stringResource(R.string.accent_color_tip),
          false
        ) {

        }

        var coloredNaviBar by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.navigation_color),
          stringResource(R.string.navigation_is_show),
          coloredNaviBar
        ) {

        }

        SettingTitle(R.string.library)

        NormalPreference(
          stringResource(R.string.library_category),
          stringResource(R.string.configure_library_category)
        ) { }

        SettingTitle(R.string.playing_screen)

        NormalPreference(
          stringResource(R.string.now_playing_screen_background),
          stringResource(R.string.now_playing_screen_theme)
        ) { }

        NormalPreference(
          stringResource(R.string.show_on_bottom),
          stringResource(R.string.show_of_bottom_tip)
        ) { }

        var screenAlwaysOn by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.screen_always_on_title),
          stringResource(R.string.screen_always_on_tip),
          screenAlwaysOn
        ) {

        }

        SettingTitle(R.string.cover)

        var ignoreMediaStore by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.ignore_mediastore_artwork),
          stringResource(R.string.ignore_mediastore_artwork_tips),
          ignoreMediaStore
        ) {
        }

        NormalPreference(
          stringResource(R.string.auto_download_album_artist_cover),
          stringResource(R.string.always)
        ) { }

        NormalPreference(
          stringResource(R.string.cover_download_source),
          stringResource(R.string.cover_download_from_lastfm)
        ) { }

        SettingTitle(R.string.lrc)
        var desktopLyric by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.float_lrc),
          stringResource(R.string.opened_desktop_lrc),
          desktopLyric
        ) {
        }

        var statusBarLyric by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.statusbar_lrc),
          checked = statusBarLyric
        ) {
        }

        NormalPreference(
          stringResource(R.string.lrc_priority),
          stringResource(R.string.lrc_priority_tip)
        ) { }

        SettingTitle(R.string.notify)

        var classicNotification by remember { mutableStateOf(false) }
        SwitchPreference(
          stringResource(R.string.notify_style),
          stringResource(R.string.notify_style_tip),
          classicNotification
        ) {
        }

        NormalPreference(
          stringResource(R.string.notify_bg_color),
          stringResource(R.string.notify_bg_color_info)
        ) { }


        SettingTitle(R.string.other)

        ArrowPreference(R.string.eq_setting) {
        }

        ArrowPreference(R.string.feedback_info) {
        }

        ArrowPreference(R.string.about_info) {
        }

        Preference(onClick = {}, title = stringResource(R.string.check_update))
        Preference(onClick = {}, title = stringResource(R.string.clear_cache)) {
          TextSecondary(text = "0MB", fontSize = 14.sp)
        }

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

  BreakPointLogic()

  ShakeLogic()

  ShowDisplayNameLogic()

  ForceSortLogic()
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