package remix.myplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import remix.myplayer.db.room.model.PlayQueue

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayQueueDao {

  @Query(
    """
    SELECT * FROM PlayQueue
  """
  )
  fun selectAll(): Flow<List<PlayQueue>>

  @Query(
    """
    DELETE FROM PlayQueue
    WHERE audio_id IN (:audioIds)
  """
  )
  suspend fun deleteSongs(audioIds: List<Long>): Int

  @Delete
  suspend fun delete(queues: List<PlayQueue>): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(playQueue: List<PlayQueue>): LongArray

  @Query(
    """
    DELETE FROM PlayQueue
  """
  )
  suspend fun clear(): Int

}