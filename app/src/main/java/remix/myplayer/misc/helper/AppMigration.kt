package remix.myplayer.misc.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.prefs.SettingPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMigration @Inject constructor(
  private val settingPrefs: SettingPrefs,
  private val database: AppDatabase
) {

  fun check() {
    if (!settingPrefs.checkMigration16600) {
      settingPrefs.checkMigration16600 = true
      CoroutineScope(Dispatchers.Main).launch {
        database.playQueueDao().clear()
      }
    }

    if (!settingPrefs.checkMigration20100) {
      settingPrefs.checkMigration20100 = true

      if (settingPrefs.songSortOrder == "date_added") {
        settingPrefs.songSortOrder = SortOrder.DATE
      } else if (settingPrefs.songSortOrder == "date_added desc") {
        settingPrefs.songSortOrder = SortOrder.DATE_DESC
      }
    }

  }
}