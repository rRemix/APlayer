package remix.myplayer.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import remix.myplayer.util.ext.activityViewModel
import remix.myplayer.viewmodel.settings.SettingViewModel

val LocalLibraryViewModel = compositionLocalOf<LibraryViewModel> {
  error("LibraryViewModel not provided")
}
val LocalSettingViewModel = compositionLocalOf<SettingViewModel> {
  error("SettingViewModel not provided")
}
val LocalTagEditViewModel = compositionLocalOf<TagEditViewModel> {
  error("TagEditViewModel not provided")
}
val LocalMainViewModel = compositionLocalOf<MainViewModel> {
  error("MainViewModel not provided")
}
val LocalTimerViewModel = compositionLocalOf<TimerViewModel> {
  error("TimerViewModel not provided")
}
val LocalWebDavViewModel = compositionLocalOf<WebDavViewModel> {
  error("WebDavViewModel not provided")
}
val LocalSmbViewModel = compositionLocalOf<SmbViewModel> {
  error("SmbViewModel not provided")
}
val LocalPlaybackViewModel = compositionLocalOf<PlaybackViewModel> {
  error("PlaybackViewModel not provided")
}


@Composable
fun ProvideViewModels(content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalLibraryViewModel provides activityViewModel(),
    LocalSettingViewModel provides activityViewModel(),
    LocalTagEditViewModel provides activityViewModel(),
    LocalMainViewModel provides activityViewModel(),
    LocalTimerViewModel provides activityViewModel(),
    LocalWebDavViewModel provides activityViewModel(),
    LocalSmbViewModel provides activityViewModel(),
    LocalPlaybackViewModel provides activityViewModel()
  ) {
    content()
  }
}

val mainViewModel: MainViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalMainViewModel.current

val libraryViewModel: LibraryViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalLibraryViewModel.current

val settingViewModel: SettingViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalSettingViewModel.current

val tagEditViewModel: TagEditViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalTagEditViewModel.current

val timerViewModel: TimerViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalTimerViewModel.current

val webDavViewModel: WebDavViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalWebDavViewModel.current

val playbackViewModel: PlaybackViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalPlaybackViewModel.current

val smbViewModel: SmbViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalSmbViewModel.current
