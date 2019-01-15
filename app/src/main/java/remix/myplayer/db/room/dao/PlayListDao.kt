package remix.myplayer.db.room.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import remix.myplayer.db.room.model.PlayList

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayListDao {
  @Insert(onConflict = OnConflictStrategy.FAIL)
  fun insertPlayList(playlist: PlayList): Long

  @Query("""
    SELECT * FROM PlayList
  """)
  fun selectAll(): List<PlayList>
}