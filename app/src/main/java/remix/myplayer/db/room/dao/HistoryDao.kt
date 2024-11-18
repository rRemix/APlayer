package remix.myplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import remix.myplayer.db.room.model.History


/**
 * Created by remix on 2019/1/12
 */
@Dao
interface HistoryDao {
  @Insert(onConflict = REPLACE)
  fun insertHistory(histories: List<History>): LongArray

  @Insert(onConflict = REPLACE)
  fun insertHistory(history: History): Long

  @Query("""
    SELECT * FROM History ORDER BY
    CASE :orderBY WHEN 'last_play' THEN last_play  END asc,
    CASE :orderBY WHEN 'last_play desc' THEN last_play END desc,
    CASE :orderBY WHEN 'play_count' THEN play_count END asc,
    CASE :orderBY WHEN 'play_count desc' THEN play_count END desc
  """)
  fun selectAll(orderBY: String): Flow<List<History>>

  @Query("""
    SELECT * FROM History
    WHERE audio_id = :audioId
  """)
  fun selectByAudioId(audioId: Long): History?

  @Update
  fun update(history: History): Int
}