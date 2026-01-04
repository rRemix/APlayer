package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.PlayList

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayListDao {

  @Query("SELECT * FROM PlayList WHERE id = 1 LIMIT 1")
  suspend fun getFavorite(): PlayList?

  @Query("SELECT EXISTS(SELECT * FROM PlayList WHERE name = :name)")
  suspend fun exists(name: String): Boolean

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(playlist: PlayList): Long

  @Query(
    """
    SELECT * FROM PlayList
    WHERE name = :name
  """
  )
  suspend fun selectByName(name: String): PlayList?

  @Update
  suspend fun update(playlist: PlayList): Int

  @Update
  suspend fun update(playlist: List<PlayList>): Int

  @Query(
    """
    DELETE FROM PlayList
    WHERE id = :id
  """
  )
  suspend fun delete(id: Long): Int

  @Query(
    """
    SELECT * FROM PlayList ORDER BY
    CASE :orderBY WHEN 'name' THEN name END asc,
    CASE :orderBY WHEN 'name desc' THEN name END desc,
    CASE :orderBY WHEN 'date' THEN date END asc,
    CASE :orderBY WHEN 'date desc' THEN date END desc
    """
  )
  fun selectAll(orderBY: String): Flow<List<PlayList>>

  @Query(
    """
    DELETE FROM PlayList
  """
  )
  suspend fun clear(): Int

}