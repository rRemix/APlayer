package remix.myplayer.db.room.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import remix.myplayer.db.room.model.PlayQueue

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayQueueDao {
  @Insert(onConflict = OnConflictStrategy.FAIL)
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
  fun deleteSongs(audioIds: List<Int>): Int


  @Query("""
    DELETE FROM PlayQueue
  """)
  fun clear(): Int
}