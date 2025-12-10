package remix.myplayer.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import remix.myplayer.data.db.room.dao.HistoryDao
import remix.myplayer.data.db.room.entity.History
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.historySortOrderFlow
import javax.inject.Inject

interface HistoryRepository {

  fun allHistories(): Flow<List<History>>

  suspend fun update(audioId: Long, checkDuplicate: Boolean = true): Int

  suspend fun clear()
}

class HistoryRepoImpl @Inject constructor(
  private val historyDao: HistoryDao,
  private val settingPrefs: SettingPrefs
) : HistoryRepository, AbstractRepository(settingPrefs), CoroutineScope by MainScope() {

  override fun allHistories(): Flow<List<History>> {
    return settingPrefs
      .historySortOrderFlow()
      .flatMapLatest { sortOrder ->
        historyDao.selectAll(sortOrder)
      }
  }

  private var lastHistoryAudioId: Long? = null

  override suspend fun update(audioId: Long, checkDuplicate: Boolean): Int {
    if (checkDuplicate && audioId == lastHistoryAudioId) {
      return 0
    }
    lastHistoryAudioId = audioId

    val currentTime = System.currentTimeMillis()
    
    // 先判断是否存在
    val existingHistory = historyDao.selectByAudioId(audioId)
    
    return if (existingHistory != null) {
      // 如果存在则更新 play_count 和 last_play
      val updatedHistory = existingHistory.copy(
        play_count = existingHistory.play_count + 1,
        last_play = currentTime
      )
      historyDao.update(updatedHistory)
    } else {
      // 如果不存在，创建新的历史记录
      val newHistory = History(
        id = 0,
        audio_id = audioId,
        play_count = 1,
        last_play = currentTime
      )
      historyDao.insertHistory(newHistory).toInt()
    }
  }

  override suspend fun clear() = historyDao.clear()
}