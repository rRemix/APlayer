package remix.myplayer.viewmodel.settings

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.ui.screen.playing.PlayingCoverAnimationStyle

@Stable
data class CommonSettings(
  val scanSize: Int,
  val lockScreen: Int,
  val manualScanFolder: String,
  val blacklist: Set<String>,
  val deleteIds: Set<String>,
  val language: Int,
  val uiFontScale: Float,
  val shake: Boolean,
  val showDisplayName: Boolean,
)

@Stable
data class PlaySettings(
  val ignoreAudioFocus: Boolean,
  val decoderMode: Int,
  val playAtBreakPoint: Boolean,
  val crossFade: Boolean,
  val autoPlay: Int,
  val speed: String,
  val listLoop: Boolean,
)

@Stable
data class ColorSettings(
  val primaryColor: Color,
  val secondaryColor: Color,
  val darkTheme: String,
  val blackTheme: Boolean,
  val coloredNaviBar: Boolean,
)

@Stable
data class LibrarySettings(
  val songSortOrder: String,
  val albumSortOrder: String,
  val artistSortOrder: String,
  val playlistSortOrder: String,
  val genreSortOrder: String,
  val folderSortOrder: String,
  val historySortOrder: String,
  val albumDetailSortOrder: String,
  val artistDetailSortOrder: String,
  @Deprecated("use SettingPrefs.getPlayListDetailSortOrder(playlistId) instead")
  val playListDetailSortOrder: String,
  val genreDetailSortOrder: String,
  val folderDetailSortOrder: String,
  val albumMode: Int,
  val artistMode: Int,
  val genreMode: Int,
  val playlistMode: Int,
)

@Stable
data class PlayingScreenSettings(
  val background: Int,
  val bottom: Int,
  val keepScreenOn: Boolean,
)

@Stable
data class CoverSettings(
  val ignoreMediaStore: Boolean,
  val autoDownloadCover: Int,
  val downloadSource: Int,
  val coverAnimationStyle: PlayingCoverAnimationStyle,
  val coverAnimationSpeed: Float,
)

@Stable
data class LyricSettings(
  val desktopLyricEnabled: Boolean,
  val statusBarLyricEnabled: Boolean,
  val translationEnabled: Boolean,
  val fontScale: Float,
  val generalLyricOrder: List<LyricOrder>,
)

@Stable
data class NotificationSettings(
  val classicNotify: Boolean,
  val notifyUseSystemBackground: Boolean,
)

@Stable
data class SettingsState(
  val common: CommonSettings,
  val play: PlaySettings,
  val color: ColorSettings,
  val library: LibrarySettings,
  val playingScreen: PlayingScreenSettings,
  val cover: CoverSettings,
  val lyric: LyricSettings,
  val notification: NotificationSettings,
)
