package remix.myplayer.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.model.misc.Library
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.data.prefs.SettingPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMigration @Inject constructor(
  private val settingPrefs: SettingPrefs,
  private val lyricPrefs: LyricPrefs,
  private val database: AppDatabase
) {

  fun check() {
    if (!settingPrefs.checkMigration16600) {
      settingPrefs.checkMigration16600 = true
      CoroutineScope(Dispatchers.Main).launch {
        database.playQueueDao().clear()
      }
    }

    if (!settingPrefs.checkMigration20500) {
      // 1.Fix invalid sort orders to defaults.
      settingPrefs.checkMigration20500 = true

      fun fixSortOrder(current: String, valid: List<String>, fallback: String): String {
        return if (valid.contains(current)) current else fallback
      }

      settingPrefs.songSortOrder =
        fixSortOrder(
          settingPrefs.songSortOrder,
          Library(Library.TAG_SONG).sortOrders,
          SortOrder.SONG_A_Z
        )
      settingPrefs.albumSortOrder =
        fixSortOrder(
          settingPrefs.albumSortOrder,
          Library(Library.TAG_ALBUM).sortOrders,
          SortOrder.ALBUM_A_Z
        )
      settingPrefs.artistSortOrder =
        fixSortOrder(
          settingPrefs.artistSortOrder,
          Library(Library.TAG_ARTIST).sortOrders,
          SortOrder.ARTIST_A_Z
        )
      settingPrefs.playlistSortOrder =
        fixSortOrder(
          settingPrefs.playlistSortOrder,
          Library(Library.TAG_PLAYLIST).sortOrders,
          SortOrder.PLAYLIST_DATE
        )
      settingPrefs.genreSortOrder =
        fixSortOrder(
          settingPrefs.genreSortOrder,
          Library(Library.TAG_GENRE).sortOrders,
          SortOrder.GENRE_A_Z
        )

      val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
      }
      val libraries = try {
        jsonParser.decodeFromString<List<Library>>(settingPrefs.libraryJson)
      } catch (_: Exception) {
        emptyList()
      }

      // 2.Migration library config
      val inputTags = libraries.map { it.tag }.toSet()
      val allTags = Library.default.map { it.tag }.toSet()

      val migratedList = if (libraries.isNotEmpty() && !inputTags.containsAll(allTags)) {
        Library.default.map { defaultLib ->
          val found = libraries.find { it.tag == defaultLib.tag }
          if (found != null) {
            defaultLib.copy(enable = found.enable)
          } else {
            defaultLib.copy(enable = false)
          }
        }
      } else {
        libraries
      }

      settingPrefs.libraryJson = Json.encodeToString(migratedList)
    }

    if (!settingPrefs.checkMigration21100) {
      settingPrefs.checkMigration21100 = true

      lyricPrefs.translationEnabled = LanguageHelper.isChinese()
    }
  }
}
