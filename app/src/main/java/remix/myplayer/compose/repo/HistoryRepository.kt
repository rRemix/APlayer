package remix.myplayer.compose.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.db.room.dao.HistoryDao
import remix.myplayer.db.room.model.History
import javax.inject.Inject

interface HistoryRepository {

  fun getAllHistories(sortOrder: String): Flow<List<History>>
}

class HistoryRepoImpl @Inject constructor(
  private val historyDao: HistoryDao,
  settingPrefs: SettingPrefs
) : HistoryRepository, AbstractRepository(settingPrefs), CoroutineScope by MainScope() {

  override fun getAllHistories(sortOrder: String) = historyDao.selectAll(sortOrder)
}