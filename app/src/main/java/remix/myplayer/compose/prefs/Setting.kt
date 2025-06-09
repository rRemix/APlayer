package remix.myplayer.compose.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.helper.LanguageHelper.AUTO
import remix.myplayer.helper.SortOrder
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.SPUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Setting @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context) {
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

  var exitAfterTimerFinish by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_EXIT_AFTER_FINISH, false)
  var timerStartAuto by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_DEFAULT, false)
  var timerDefaultDuration by PrefsDelegate(sp, SPUtil.SETTING_KEY.TIMER_DURATION, -1)
}