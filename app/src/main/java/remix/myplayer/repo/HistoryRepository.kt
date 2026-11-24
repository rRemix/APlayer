package remix.myplayer.repo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import remix.myplayer.data.db.room.dao.HistoryDao
import remix.myplayer.data.db.room.entity.History
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.historySortOrderFlow
import javax.inject.Inject

interface HistoryRepository {
  fun allHistories(): Flow<List<History>>
  suspend fun update(key: SongIdentity, checkDuplicate: Boolean = true): Int
  suspend fun clear()
}
class HistoryRepoImpl @Inject constructor(
  private val historyDao: HistoryDao,
  private val settingPrefs: SettingPrefs
) : HistoryRepository {
  private val mutex = Mutex()
  private var lastIdentity: SongIdentity? = null
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun allHistories(): Flow<List<History>> = settingPrefs.historySortOrderFlow()
    .flatMapLatest { historyDao.selectAll(it) }
  override suspend fun update(key: SongIdentity, checkDuplicate: Boolean): Int = mutex.withLock {
    if (checkDuplicate && key == lastIdentity) return@withLock 0
    val result = historyDao.record(key, System.currentTimeMillis())
    lastIdentity = key
    result
  }
  override suspend fun clear() = mutex.withLock { historyDao.clear(); lastIdentity = null }
}
