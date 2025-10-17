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

  suspend fun update(audioId: Long): Int
}

class HistoryRepoImpl @Inject constructor(
  private val historyDao: HistoryDao,
  settingPrefs: SettingPrefs
) : HistoryRepository, AbstractRepository(settingPrefs), CoroutineScope by MainScope() {

  override fun getAllHistories(sortOrder: String) = historyDao.selectAll(sortOrder)

  override suspend fun update(audioId: Long): Int {
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
}