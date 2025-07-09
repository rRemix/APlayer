package remix.myplayer.db.room.dao

import androidx.room.Dao
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
  @Query("""
    SELECT * FROM PlayQueue
  """)
  fun selectAllSuspend(): Flow<List<PlayQueue>>

  @Query("""
    DELETE FROM PlayQueue
    WHERE audio_id IN (:audioIds)
  """)
  suspend fun deleteSongsSuspend(audioIds: List<Long>): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertPlayQueueSuspend(playQueue: List<PlayQueue>): LongArray

  @Insert(onConflict = OnConflictStrategy.ABORT)
  fun insertPlayQueue(playQueue: List<PlayQueue>): LongArray

  @Insert(onConflict = OnConflictStrategy.FAIL)
  fun insertPlayQueue(playListSongs: PlayQueue): Long

  @Query("""
    SELECT * FROM PlayQueue
  """)
  fun selectAll(): List<PlayQueue>

  @Query("""
    DELETE FROM PlayQueue
    WHERE audio_id IN (:audioIds)
  """)
  fun deleteSongs(audioIds: List<Long>): Int

  @Query("""
    DELETE FROM PlayQueue
  """)
  fun clear(): Int
}