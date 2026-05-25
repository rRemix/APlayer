package remix.myplayer.ui.screen.setting

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.helper.EQHelper
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteAbout
import remix.myplayer.ui.nav.RouteSettingDetail
import remix.myplayer.ui.screen.setting.logic.color.BlackThemeLogic
import remix.myplayer.ui.screen.setting.logic.color.ColoredNaviBarLogic
import remix.myplayer.ui.screen.setting.logic.color.DarkThemeLogic
import remix.myplayer.ui.screen.setting.logic.color.PrimaryColorLogic
import remix.myplayer.ui.screen.setting.logic.color.SecondaryColorLogic
import remix.myplayer.ui.screen.setting.logic.common.BlackListLogic
import remix.myplayer.ui.screen.setting.logic.common.BreakPointLogic
import remix.myplayer.ui.screen.setting.logic.common.ExportPlayListLogic
import remix.myplayer.ui.screen.setting.logic.common.ImportPlayListLogic
import remix.myplayer.ui.screen.setting.logic.common.LanguageLogic
import remix.myplayer.ui.screen.setting.logic.common.LockScreenLogic
import remix.myplayer.ui.screen.setting.logic.common.ManualScanLogic
import remix.myplayer.ui.screen.setting.logic.common.RestoreDeleteLogic
import remix.myplayer.ui.screen.setting.logic.common.ScanSizeLogic
import remix.myplayer.ui.screen.setting.logic.common.ShakeLogic
import remix.myplayer.ui.screen.setting.logic.common.ShowDisplayNameLogic
import remix.myplayer.ui.screen.setting.logic.common.UiFontScaleLogic
import remix.myplayer.ui.screen.setting.logic.cover.AutoDownloadLogic
import remix.myplayer.ui.screen.setting.logic.cover.DownloadSourceLogic
import remix.myplayer.ui.screen.setting.logic.cover.IgnoreMediaStoreLogic
import remix.myplayer.ui.screen.setting.logic.library.LibraryLogic
import remix.myplayer.ui.screen.setting.logic.lyric.DesktopLyricLogic
import remix.myplayer.ui.screen.setting.logic.lyric.LyricPriorityLogic
import remix.myplayer.ui.screen.setting.logic.lyric.StatusBarLyricLogic
import remix.myplayer.ui.screen.setting.logic.lyric.TranslationLogic
import remix.myplayer.ui.screen.setting.logic.notification.ClassicNotifyLogic
import remix.myplayer.ui.screen.setting.logic.notification.NotifyBackgroundLogic
import remix.myplayer.ui.screen.setting.logic.other.ClearCacheLogic
import remix.myplayer.ui.screen.setting.logic.play.AutoPlayLogic
import remix.myplayer.ui.screen.setting.logic.play.DecoderModeLogic
import remix.myplayer.ui.screen.setting.logic.play.IgnoreAudioFocusLogic
import remix.myplayer.ui.screen.setting.logic.play.ListLoopLogic
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
      modifier = Modifier.padding(contentPadding)
    ) {
      item {
        val nav = LocalNavController.current

        SettingCategory.entries.forEach { category ->
          SettingCategoryPreference(
            iconRes = category.iconRes,
            titleRes = category.titleRes,
            descriptionRes = category.descriptionRes,
          ) {
            nav.navigate(settingDetailRoute(category.route))
          }
        }
      }
    }
  }
}

@Composable
fun SettingDetailScreen(categoryKey: String) {
  val category = SettingCategory.fromRoute(categoryKey) ?: return

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(category.titleRes), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    LazyColumn(
      modifier = Modifier.padding(contentPadding)
    ) {
      item {
        when (category) {
          SettingCategory.Common -> CommonPreferenceItems()
          SettingCategory.Play -> PlayPreferenceItems()
          SettingCategory.Color -> ColorPreferenceItems()
          SettingCategory.Library -> LibraryPreferenceItems()
          SettingCategory.PlayingScreen -> PlayingScreenPreferenceItems()
          SettingCategory.Cover -> CoverPreferenceItems()
          SettingCategory.Lyric -> LyricPreferenceItems()
          SettingCategory.Notification -> NotificationPreferenceItems()
          SettingCategory.Other -> OtherPreferenceItems()
        }
      }
    }
  }
}

@Composable
private fun SettingCategoryPreference(
  @DrawableRes iconRes: Int,
  @StringRes titleRes: Int,
  @StringRes descriptionRes: Int,
  onClick: () -> Unit,
) {
  val title = stringResource(titleRes)
  Preference(
    onClick = onClick,
    title = stringResource(titleRes),
    content = stringResource(descriptionRes),
    leading = {
      Icon(
        modifier = Modifier.padding(end = 24.dp),
        painter = painterResource(iconRes),
        contentDescription = title,
        tint = LocalTheme.current.primary
      )
    }
  )
}

private fun settingDetailRoute(categoryRoute: String): String {
  return "$RouteSettingDetail/$categoryRoute"
}

private enum class SettingCategory(
  @get:DrawableRes val iconRes: Int,
  @get:StringRes val titleRes: Int,
  @get:StringRes val descriptionRes: Int,
  val route: String,
) {

  Common(R.drawable.ic_tune_24dp, R.string.common, R.string.setting_common_desc, "common"),
  Play(R.drawable.ic_play_arrow_black_24dp, R.string.play, R.string.setting_play_desc, "play"),
  Color(R.drawable.ic_palette_24dp, R.string.color, R.string.setting_color_desc, "color"),
  Library(
    R.drawable.ic_library_books_24dp,
    R.string.library,
    R.string.setting_library_desc,
    "library"
  ),
  PlayingScreen(
    R.drawable.ic_smart_display_24dp,
    R.string.playing_screen,
    R.string.setting_playing_screen_desc,
    "playing_screen"
  ),
  Cover(R.drawable.ic_album_24dp, R.string.cover, R.string.setting_cover_desc, "cover"),
  Lyric(R.drawable.ic_lyrics_24dp, R.string.lrc, R.string.setting_lyric_desc, "lyric"),
  Notification(
    R.drawable.ic_notification_sound_24dp,
    R.string.notify,
    R.string.setting_notification_desc,
    "notification"
  ),
  Other(R.drawable.ic_info_outlined_24dp, R.string.other, R.string.setting_other_desc, "other");

  companion object {

    fun fromRoute(route: String): SettingCategory? {
      return entries.firstOrNull { it.route == route }
    }
  }
}

@Composable
private fun CommonPreferenceItems() {
  ScanSizeLogic()

  BlackListLogic()

  LockScreenLogic()

  ManualScanLogic()

  ImportPlayListLogic()

  ExportPlayListLogic()

  RestoreDeleteLogic()

  LanguageLogic()

  UiFontScaleLogic()

  ShakeLogic()

  ShowDisplayNameLogic()
}

@Composable
private fun PlayPreferenceItems() {
  IgnoreAudioFocusLogic()

  BreakPointLogic()

  PlayFadeLogic()

  ListLoopLogic()

  AutoPlayLogic()

  DecoderModeLogic()
}

@Composable
private fun ColorPreferenceItems() {
  DarkThemeLogic()

  BlackThemeLogic()

  PrimaryColorLogic()

  SecondaryColorLogic()

  ColoredNaviBarLogic()

}

@Composable
private fun LibraryPreferenceItems() {
  LibraryLogic()
}

@Composable
private fun PlayingScreenPreferenceItems() {
  PlayingScreenBackgroundLogic()

  PlayingScreenBottomLogic()

  KeepScreenOnLogic()
}

@Composable
private fun CoverPreferenceItems() {
  PlayingCoverAnimationLogic()

  IgnoreMediaStoreLogic()

  AutoDownloadLogic()

  DownloadSourceLogic()
}

@Composable
private fun NotificationPreferenceItems() {
  ClassicNotifyLogic()

  NotifyBackgroundLogic()
}

@Composable
private fun LyricPreferenceItems() {
  DesktopLyricLogic()

  TranslationLogic()

  StatusBarLyricLogic()

  LyricPriorityLogic()
}

@Composable
private fun OtherPreferenceItems() {
  val mainViewModel = mainViewModel
  val activity = LocalActivity.current
  val nav = LocalNavController.current

  ArrowPreference(R.string.eq_setting) {
    EQHelper.startEqualizer(activity ?: return@ArrowPreference, nav)
  }

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
