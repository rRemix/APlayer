package remix.myplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import remix.myplayer.db.room.model.WebDav

@Dao
interface WebDavDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(WebDav: WebDav): Long

  @Query(
    """
    SELECT * from WebDav ORDER BY createAt DESC
  """
  )
  fun queryAll() : Flow<List<WebDav>>

  @Delete
  suspend fun delete(webDav: WebDav): Int
}