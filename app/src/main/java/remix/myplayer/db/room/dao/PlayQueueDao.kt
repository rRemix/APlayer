package remix.myplayer.db.room.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import remix.myplayer.db.room.model.PlayQueue

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayQueueDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  fun insertPlayListSong(playQueue: List<PlayQueue>): LongArray

  @Insert(onConflict = OnConflictStrategy.ABORT)
  fun insertPlayQueue(playListSongs: PlayQueue): Long
}