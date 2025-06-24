package remix.myplayer.compose.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.glide.UriFetcher.DOWNLOAD_LASTFM
import remix.myplayer.helper.LanguageHelper.AUTO
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.SPUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "Setting") {
  var libraryJson by PrefsDelegate(sp, SPUtil.SETTING_KEY.LIBRARY, "")

  var scanSize by PrefsDelegate(sp, SPUtil.SETTING_KEY.SCAN_SIZE, MB)
  var forceSort by PrefsDelegate(sp, SPUtil.SETTING_KEY.FORCE_SORT, false)

  var songSortOrder by PrefsDelegate(sp, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SONG_A_Z)
  var albumSortOrder by PrefsDelegate(sp, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, SortOrder.ALBUM_A_Z)
  var artistSortOrder by PrefsDelegate(sp, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, SortOrder.ARTIST_A_Z)
  var playlistSortOrder by PrefsDelegate(sp, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER, SortOrder.PLAYLIST_DATE)
  var genreSortOrder by PrefsDelegate(sp, SPUtil.SETTING_KEY.GENRE_SORT_ORDER, SortOrder.GENRE_A_Z)

  var albumMode by PrefsDelegate(sp, SPUtil.SETTING_KEY.MODE_FOR_ALBUM, HeaderAdapter.GRID_MODE)
  var artistMode by PrefsDelegate(sp, SPUtil.SETTING_KEY.MODE_FOR_ARTIST, HeaderAdapter.GRID_MODE)
  var genreMode by PrefsDelegate(sp, SPUtil.SETTING_KEY.MODE_FOR_GENRE, HeaderAdapter.GRID_MODE)
  var playlistMode by PrefsDelegate(sp, SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST, HeaderAdapter.GRID_MODE)

  var deleteIds by PrefsDelegate(sp, SPUtil.SETTING_KEY.BLACKLIST_SONG, emptySet<String>())
  var blacklist by PrefsDelegate(sp, SPUtil.SETTING_KEY.BLACKLIST, emptySet<String>())

  var lockScreen by PrefsDelegate(sp, defaultVal = Constants.APLAYER_LOCKSCREEN)
  var language by PrefsDelegate(sp, defaultVal = AUTO)
  var playAtBreakPoint by PrefsDelegate(sp, SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false)
  var shake by PrefsDelegate(sp, SPUtil.SETTING_KEY.SHAKE, false)
  var showDisplayName by PrefsDelegate(sp, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME, false)

  var ignoreAudioFocus by PrefsDelegate(sp, SPUtil.SETTING_KEY.AUDIO_FOCUS, false)
  var autoPlay by PrefsDelegate(sp, SPUtil.SETTING_KEY.AUTO_PLAY, HeadsetPlugReceiver.NEVER)
  var playFade by PrefsDelegate(sp, SPUtil.SETTING_KEY.CROSS_FADE, false)

  var playingScreenBackground by PrefsDelegate(sp, SPUtil.SETTING_KEY.PLAYER_BACKGROUND, BACKGROUND_ADAPTIVE_COLOR)
  var playingScreenBottom by PrefsDelegate(sp, SPUtil.SETTING_KEY.PLAYER_BACKGROUND, BOTTOM_SHOW_BOTH)
  var keepScreenOn by PrefsDelegate(sp, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON, false)

  var ignoreMediaStore by PrefsDelegate(sp, SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE, false)
  var autoDownloadCover by PrefsDelegate(sp,SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER, DOWNLOAD_COVER_ALWAYS)
  var downloadSource by PrefsDelegate(sp, SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, DOWNLOAD_LASTFM)

  var desktopLyric by PrefsDelegate(sp, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
  var statusBarLyric by PrefsDelegate(sp, SPUtil.SETTING_KEY.STATUSBAR_LYRIC_SHOW, false)

  var classicNotify by PrefsDelegate(sp, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)
  var notifyUseSystemBackground by PrefsDelegate(sp, SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR, true)

  var exitAfterTimerFinish by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_EXIT_AFTER_FINISH, false)
  var timerStartAuto by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_DEFAULT, false)
  var timerDefaultDuration by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_DURATION, -1)

  companion object {
    const val BOTTOM_SHOW_NEXT = 0
    const val BOTTOM_SHOW_VOLUME = 1
    const val BOTTOM_SHOW_BOTH = 2
    const val BOTTOM_SHOW_NONE = 3

    const val BACKGROUND_THEME = 0
    const val BACKGROUND_ADAPTIVE_COLOR = 1
    const val BACKGROUND_CUSTOM_IMAGE = 2

    const val DOWNLOAD_COVER_ALWAYS = 0
    const val DOWNLOAD_COVER_WIFI_ONLY = 1
    const val DOWNLOAD_COVER_NEVER = 2

    const val CLASSIC_NOTIFY_BACKGROUND_SYSTEM = 0
  }
}