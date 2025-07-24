package remix.myplayer.compose.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import remix.myplayer.compose.activityViewModel

val LocalLibraryViewModel = compositionLocalOf<LibraryViewModel> {
  error("LibraryViewModel not provided")
}
val LocalMusicViewModel = compositionLocalOf<MusicViewModel> {
  error("MusicViewModel not provided")
}
val LocalSettingViewModel = compositionLocalOf<SettingViewModel> {
  error("SettingViewModel not provided")
}
val LocalMainViewModel = compositionLocalOf<MainViewModel> {
  error("MainViewModel not provided")
}
val LocalPlayingViewModel = compositionLocalOf<PlayingViewModel> {
  error("PlayingViewModel not provided")
}
val LocalTimerViewModel = compositionLocalOf<TimerViewModel> {
  error("TimerViewModel not provided")
}

@Composable
fun ProvideViewModels(content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalLibraryViewModel provides activityViewModel(),
    LocalMusicViewModel provides activityViewModel(),
    LocalSettingViewModel provides activityViewModel(),
    LocalMainViewModel provides activityViewModel(),
    LocalPlayingViewModel provides activityViewModel(),
    LocalTimerViewModel provides activityViewModel(),
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

val musicViewModel: MusicViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalMusicViewModel.current

val settingViewModel: SettingViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalSettingViewModel.current

val playingViewModel: PlayingViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalPlayingViewModel.current

val timerViewModel: TimerViewModel
  @Composable
  @ReadOnlyComposable
  get() = LocalTimerViewModel.current
