package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.History
import remix.myplayer.data.model.audio.SongIdentity

@Dao
interface HistoryDao {
  @Query("""SELECT * FROM History ORDER BY
    CASE :orderBY WHEN 'last_play' THEN last_play END asc,
    CASE :orderBY WHEN 'last_play desc' THEN last_play END desc,
    CASE :orderBY WHEN 'play_count' THEN play_count END asc,
    CASE :orderBY WHEN 'play_count desc' THEN play_count END desc""")
  fun selectAll(orderBY: String): Flow<List<History>>
  @Query("SELECT * FROM History WHERE source_id = :source AND source_key = :key LIMIT 1")
  suspend fun find(source: String, key: String): History?
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(history: History): Long
  @Transaction
  suspend fun record(key: SongIdentity, time: Long): Int {
    val old = find(key.sourceId.value, key.sourceKey)
    val count = ((old?.play_count ?: 0).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    insert(History(key.sourceId.value, key.sourceKey, count, time))
    return 1
  }
  @Query("DELETE FROM History")
  suspend fun clear()
}
