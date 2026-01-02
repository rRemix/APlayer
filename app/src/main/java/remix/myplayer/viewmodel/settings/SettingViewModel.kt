package remix.myplayer.viewmodel.settings

import android.app.Activity
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.Library
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.lyric.LyricManager
import remix.myplayer.misc.helper.ShakeDetector
import remix.myplayer.misc.updateIf
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.usecase.DeleteSongUseCase
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.DeleteSongState
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.ImportPlayListState
import remix.myplayer.ui.dialog.ReNamePlayListState
import remix.myplayer.ui.dialog.SongDetailState
import remix.myplayer.ui.dialog.SongEditState
import remix.myplayer.ui.dialog.runWithLoading
import remix.myplayer.ui.theme.ThemeController
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  val settingPrefs: SettingPrefs,
  val lyricPrefs: LyricPrefs,
  val themeController: ThemeController,
  val lyricManager: LyricManager
) : ViewModel() {

  @Inject
  lateinit var deleteSongUseCase: DeleteSongUseCase

  private val _currentLibrary = MutableStateFlow(Library.defaultLibrary)
  val currentLibrary = _currentLibrary.asStateFlow()

  private val _allLibraries = MutableStateFlow(Library.allLibraries)
  val allLibraries = _allLibraries.asStateFlow()

  // 设置状态
  private val _settingsState = MutableStateFlow(loadState())
  val settingsState = _settingsState.asStateFlow()

  private fun loadState(): SettingsState = SettingsState(
    common = CommonSettings(
      scanSize = settingPrefs.scanSize,
      forceSort = settingPrefs.forceSort,
      lockScreen = settingPrefs.lockScreen,
      manualScanFolder = settingPrefs.manualScanFolder,
      blacklist = settingPrefs.blacklist,
      deleteIds = settingPrefs.deleteIds,
      language = settingPrefs.language,
      shake = settingPrefs.shake,
      showDisplayName = settingPrefs.showDisplayName
    ),
    play = PlaySettings(
      ignoreAudioFocus = settingPrefs.ignoreAudioFocus,
      playAtBreakPoint = settingPrefs.playAtBreakPoint,
      crossFade = settingPrefs.crossFade,
      autoPlay = settingPrefs.autoPlay,
      speed = settingPrefs.speed,
    ),
    color = ColorSettings(
      primaryColor = themeController.appTheme.primary,
      secondaryColor = themeController.appTheme.secondary,
      darkTheme = themeController.dark,
      blackTheme = themeController.black,
      coloredNaviBar = themeController.appTheme.coloredNaviBar
    ),
    library = LibrarySettings(
      songSortOrder = settingPrefs.songSortOrder,
      albumSortOrder = settingPrefs.albumSortOrder,
      artistSortOrder = settingPrefs.artistSortOrder,
      playlistSortOrder = settingPrefs.playlistSortOrder,
      genreSortOrder = settingPrefs.genreSortOrder,
      historySortOrder = settingPrefs.historySortOrder,
      albumDetailSortOrder = settingPrefs.albumDetailSortOrder,
      artistDetailSortOrder = settingPrefs.artistDetailSortOrder,
      playListDetailSortOrder = settingPrefs.playListDetailSortOrder,
      genreDetailSortOrder = settingPrefs.genreDetailSortOrder,
      folderDetailSortOrder = settingPrefs.folderDetailSortOrder,
      albumMode = settingPrefs.albumMode,
      artistMode = settingPrefs.artistMode,
      genreMode = settingPrefs.genreMode,
      playlistMode = settingPrefs.playlistMode
    ),
    playingScreen = PlayingScreenSettings(
      background = settingPrefs.playingScreenBackground,
      bottom = settingPrefs.playingScreenBottom,
      keepScreenOn = settingPrefs.keepScreenOn
    ),
    cover = CoverSettings(
      ignoreMediaStore = settingPrefs.ignoreMediaStore,
      autoDownloadCover = settingPrefs.autoDownloadCover,
      downloadSource = settingPrefs.downloadSource
    ),
    lyric = LyricSettings(
      desktopLyricEnabled = lyricPrefs.desktopLyricEnabled,
      statusBarLyricEnabled = lyricPrefs.statusBarLyricEnabled,
      fontScale = lyricPrefs.fontScale,
      generalLyricOrder = lyricPrefs.generalLyricOrderList
    ),
    notification = NotificationSettings(
      classicNotify = settingPrefs.classicNotify,
      notifyUseSystemBackground = settingPrefs.notifyUseSystemBackground
    )
  )

  init {
    // load libraries
    val libraries = try {
      Json.decodeFromString<List<Library>>(settingPrefs.libraryJson)
        .ifEmpty { Library.allLibraries }
    } catch (_: Exception) {
      Library.allLibraries
    }

    setAllLibraries(libraries)

    changeLibrary(libraries[0])
  }

  fun setAllLibraries(libraries: List<Library>) {
    _allLibraries.value = libraries
    settingPrefs.libraryJson = Json.encodeToString(libraries)
  }

  fun changeLibrary(library: Library) {
    _currentLibrary.value = library
  }

  // -------- Common 分组 ----------
  fun setScanSize(kb: Int) {
    settingPrefs.scanSize = kb
    _settingsState.update { it.copy(common = it.common.copy(scanSize = kb)) }
  }

  fun setForceSort(enabled: Boolean) {
    settingPrefs.forceSort = enabled
    _settingsState.update { it.copy(common = it.common.copy(forceSort = enabled)) }
  }

  fun setLockScreen(mode: Int) {
    settingPrefs.lockScreen = mode
    _settingsState.update { it.copy(common = it.common.copy(lockScreen = mode)) }
  }

  fun setManualScanFolder(path: String) {
    settingPrefs.manualScanFolder = path
    _settingsState.update { it.copy(common = it.common.copy(manualScanFolder = path)) }
  }

  fun setBlacklist(values: Set<String>) {
    settingPrefs.blacklist = values
    _settingsState.update { it.copy(common = it.common.copy(blacklist = values)) }
  }

  fun setDeleteIds(values: Set<String>) {
    settingPrefs.deleteIds = values
    _settingsState.update { it.copy(common = it.common.copy(deleteIds = values)) }
  }

  fun setShake(enabled: Boolean) {
    settingPrefs.shake = enabled
    _settingsState.update { it.copy(common = it.common.copy(shake = enabled)) }

    if (enabled) {
      ShakeDetector.getInstance().beginListen()
    } else {
      ShakeDetector.getInstance().stopListen()
    }
  }

  fun setShowDisplayName(enabled: Boolean) {
    settingPrefs.showDisplayName = enabled
    _settingsState.update { it.copy(common = it.common.copy(showDisplayName = enabled)) }
  }

  // -------- Play 分组 ----------
  fun setIgnoreAudioFocus(enabled: Boolean) {
    settingPrefs.ignoreAudioFocus = enabled
    _settingsState.update { it.copy(play = it.play.copy(ignoreAudioFocus = enabled)) }
  }

  fun setPlayAtBreakPoint(enabled: Boolean) {
    settingPrefs.playAtBreakPoint = enabled
    _settingsState.update { it.copy(play = it.play.copy(playAtBreakPoint = enabled)) }
  }

  fun setCrossFade(enabled: Boolean) {
    settingPrefs.crossFade = enabled
    _settingsState.update { it.copy(play = it.play.copy(crossFade = enabled)) }
  }

  fun setAutoPlay(mode: Int) {
    settingPrefs.autoPlay = mode
    _settingsState.update { it.copy(play = it.play.copy(autoPlay = mode)) }
  }

  fun setSpeed(speed: String) {
    settingPrefs.speed = speed
    _settingsState.update { it.copy(play = it.play.copy(speed = speed)) }
  }

  // -------- Color 分组 ----------
  fun setPrimaryColor(color: Color) {
    themeController.setPrimary(color)
    _settingsState.update { it.copy(color = it.color.copy(primaryColor = color)) }
  }

  fun setSecondaryColor(color: Color) {
    themeController.setSecondary(color)
    _settingsState.update { it.copy(color = it.color.copy(secondaryColor = color)) }
  }

  fun setDarkTheme(option: String) {
    themeController.dark = option
    _settingsState.update { it.copy(color = it.color.copy(darkTheme = option)) }
  }

  fun setBlackTheme(enabled: Boolean) {
    themeController.black = enabled
    _settingsState.update { it.copy(color = it.color.copy(blackTheme = enabled)) }
  }

  fun setColoredNaviBar(enabled: Boolean) {
    themeController.setColoredNaviBar(enabled)
    _settingsState.update { it.copy(color = it.color.copy(coloredNaviBar = enabled)) }
  }

  // 统一设置排序
  fun setSortOrder(category: SortCategory, order: String): Boolean {
    if (category.saveOrder(order, settingPrefs)) {
      _settingsState.update {
        val lib = it.library
        it.copy(
          library = when (category) {
            SortCategory.SONG -> lib.copy(songSortOrder = order)
            SortCategory.ALBUM -> lib.copy(albumSortOrder = order)
            SortCategory.ARTIST -> lib.copy(artistSortOrder = order)
            SortCategory.PLAYLIST -> lib.copy(playlistSortOrder = order)
            SortCategory.GENRE -> lib.copy(genreSortOrder = order)
            SortCategory.HISTORY -> lib.copy(historySortOrder = order)
            SortCategory.ALBUM_DETAIL -> lib.copy(albumDetailSortOrder = order)
            SortCategory.ARTIST_DETAIL -> lib.copy(artistDetailSortOrder = order)
            SortCategory.PLAYLIST_DETAIL -> lib.copy(playListDetailSortOrder = order)
            SortCategory.GENRE_DETAIL -> lib.copy(genreDetailSortOrder = order)
            SortCategory.FOLDER_DETAIL -> lib.copy(folderDetailSortOrder = order)
          }
        )
      }
      return true
    }

    return false
  }

  fun setAlbumMode(mode: Int) {
    if (_settingsState.value.library.albumMode == mode) return
    settingPrefs.albumMode = mode
    _settingsState.update { it.copy(library = it.library.copy(albumMode = mode)) }
  }

  fun setArtistMode(mode: Int) {
    if (_settingsState.value.library.artistMode == mode) return
    settingPrefs.artistMode = mode
    _settingsState.update { it.copy(library = it.library.copy(artistMode = mode)) }
  }

  fun setGenreMode(mode: Int) {
    if (_settingsState.value.library.genreMode == mode) return
    settingPrefs.genreMode = mode
    _settingsState.update { it.copy(library = it.library.copy(genreMode = mode)) }
  }

  fun setPlaylistMode(mode: Int) {
    if (_settingsState.value.library.playlistMode == mode) return
    settingPrefs.playlistMode = mode
    _settingsState.update { it.copy(library = it.library.copy(playlistMode = mode)) }
  }

  // -------- PlayingScreen 分组 ----------
  fun setPlayingScreenBackground(bg: Int) {
    settingPrefs.playingScreenBackground = bg
    _settingsState.update { it.copy(playingScreen = it.playingScreen.copy(background = bg)) }
  }

  fun setPlayingScreenBottom(bottom: Int) {
    settingPrefs.playingScreenBottom = bottom
    _settingsState.update { it.copy(playingScreen = it.playingScreen.copy(bottom = bottom)) }
  }

  fun setKeepScreenOn(enabled: Boolean) {
    settingPrefs.keepScreenOn = enabled
    _settingsState.update { it.copy(playingScreen = it.playingScreen.copy(keepScreenOn = enabled)) }
  }

  // -------- Cover 分组 ----------
  fun setIgnoreMediaStore(enabled: Boolean) {
    settingPrefs.ignoreMediaStore = enabled
    _settingsState.update { it.copy(cover = it.cover.copy(ignoreMediaStore = enabled)) }
  }

  fun setAutoDownloadCover(mode: Int) {
    settingPrefs.autoDownloadCover = mode
    _settingsState.update { it.copy(cover = it.cover.copy(autoDownloadCover = mode)) }
  }

  fun setDownloadSource(source: Int) {
    settingPrefs.downloadSource = source
    _settingsState.update { it.copy(cover = it.cover.copy(downloadSource = source)) }
  }

  // -------- Lyric 分组 ----------
  fun setDesktopLyricEnabled(enabled: Boolean, activity: Activity?) {
    lyricManager.setDesktopLyricEnabled(enabled, activity)
    _settingsState.update { it.copy(lyric = it.lyric.copy(desktopLyricEnabled = enabled)) }
  }

  fun setStatusBarLyricEnabled(enabled: Boolean) {
    lyricManager.isStatusBarLyricEnabled = enabled
    _settingsState.update { it.copy(lyric = it.lyric.copy(statusBarLyricEnabled = enabled)) }
  }

  fun setLyricFontScale(scale: Float) {
    lyricPrefs.fontScale = scale
    _settingsState.update { it.copy(lyric = it.lyric.copy(fontScale = scale)) }
  }

  fun setGeneralLyricOrder(orderList: List<LyricOrder>) {
    viewModelScope.launch(Dispatchers.IO) {
      // 清除歌词相关配置和缓存
      lyricPrefs.clearUserSave()
      lyricPrefs.generalLyricOrder = Json.encodeToString(orderList)
      lyricManager.clearAllCache(includePersistent = true)
      // 重新获取歌词
      lyricManager.updateLyrics(MusicStateSource.currentPlaybackUiState.song)
    }
    _settingsState.update { it.copy(lyric = it.lyric.copy(generalLyricOrder = orderList)) }
  }

  // -------- Notification 分组 ----------
  fun setClassicNotify(enabled: Boolean) {
    settingPrefs.classicNotify = enabled
    _settingsState.update { it.copy(notification = it.notification.copy(classicNotify = enabled)) }
  }

  fun setNotifyUseSystemBackground(enabled: Boolean) {
    settingPrefs.notifyUseSystemBackground = enabled
    _settingsState.update {
      it.copy(notification = it.notification.copy(notifyUseSystemBackground = enabled))
    }
  }

  private val _addSongToPlayListState =
    MutableStateFlow(ImportPlayListState(DialogState(false), DialogState(false)))
  val addSongToPlayListState = _addSongToPlayListState.asStateFlow()

  fun showAddSongToPlayListDialog(songIds: List<Long>, initialText: String = "") {
    _addSongToPlayListState.updateIf(
      condition = { !it.rootDialogState.isOpen },
      transform = {
        it.rootDialogState.show()
        it.copy(
          inputText = initialText,
          songIds = songIds
        )
      }
    )
  }

  fun updateImportPlayListState(text: String) {
    _addSongToPlayListState.update {
      it.copy(inputText = text)
    }
  }

  private val _deleteSongState =
    MutableStateFlow(DeleteSongState(deleteSource = settingPrefs.deleteSource))
  val deleteSongState = _deleteSongState.asStateFlow()

  fun showDeleteSongDialog(
    models: List<APlayerModel>,
    titleRes: Int = R.string.confirm_delete_from_library,
    parent: APlayerModel? = null
  ) {
    _deleteSongState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(
          models = models,
          titleRes = titleRes,
          parent = parent
        )
      }
    )
  }

  fun updateDeleteSongState(deleteSource: Boolean) {
    _deleteSongState.update {
      it.copy(deleteSource = deleteSource)
    }
  }

  fun deleteSongs(
    activity: BaseActivity?,
    models: List<APlayerModel>,
    deleteSource: Boolean,
    parent: APlayerModel?
  ) =
    viewModelScope.runWithLoading {
      deleteSongUseCase(activity, models, deleteSource, parent)
    }

  private val _reNamePlayListState = MutableStateFlow(ReNamePlayListState(DialogState()))
  val reNamePlayListState = _reNamePlayListState.asStateFlow()

  fun showReNamePlayListDialog(playList: PlayList) {
    _reNamePlayListState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(playList = playList)
      }
    )
  }

  private val _songDetailState = MutableStateFlow(SongDetailState(DialogState()))
  val songDetailState = _songDetailState.asStateFlow()

  fun showSongDetailDialog(song: Song) {
    _songDetailState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(song = song)
      }
    )
  }

  private val _songEditState = MutableStateFlow(SongEditState(DialogState()))
  val songEditState = _songEditState.asStateFlow()

  fun showSongEditDialog(song: Song) {
    _songEditState.updateIf(
      condition = { !it.dialogState.isOpen },
      transform = {
        it.dialogState.show()
        it.copy(song = song)
      }
    )
  }

  fun clearCache(context: Context, andThen: () -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
      context.cacheDir.deleteRecursively()
      context.externalCacheDir?.deleteRecursively()
      Glide.get(context).clearDiskCache()
      // 只清除非持久化歌词缓存，保留用户手动选择的歌词
      lyricManager.clearAllCache(includePersistent = false)
      andThen()
    }
  }
}
