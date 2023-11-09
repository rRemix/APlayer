package remix.myplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
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
    CASE WHEN :asc = 1 THEN play_count END ASC,
    CASE WHEN :asc = 0 THEN play_count END DESC
  """)
  fun selectAll(asc: Boolean): Flow<List<History>>

  @Query("""
    SELECT * FROM History
    WHERE audio_id = :audioId
  """)
  fun selectByAudioId(audioId: Long): History?

  @Update
  fun update(history: History): Int
}