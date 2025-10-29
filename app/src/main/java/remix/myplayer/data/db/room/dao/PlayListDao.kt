package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.PlayList

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayListDao {

  @Query("SELECT * FROM PlayList WHERE id = 1 LIMIT 1")
  suspend fun getFavorite(): PlayList?

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
    UPDATE PlayList 
    SET audioIds = REPLACE(
      REPLACE(
        REPLACE(audioIds, ',' || :audioId || ',', ','),
        '[' || :audioId || ',', '['),
      ',' || :audioId || ']', ']')
    WHERE audioIds LIKE '%' || :audioId || '%'
  """
  )
  suspend fun removeAudioIdFromAll(audioId: Long): Int

  @Query(
    """
    DELETE FROM PlayList
    WHERE id = :id
  """
  )
  suspend fun delete(id: Long): Int

  @RawQuery(observedEntities = [PlayList::class])
  fun selectAllOrderBy(query: SupportSQLiteQuery): Flow<List<PlayList>>

  @Query(
    """
    DELETE FROM PlayList
  """
  )
  suspend fun clear(): Int

}