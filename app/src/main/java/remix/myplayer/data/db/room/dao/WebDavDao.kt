package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.WebDav

@Dao
interface WebDavDao {
  @Query("SELECT * FROM WebDav WHERE id = :id LIMIT 1")
  suspend fun byId(id: Int): WebDav?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(webdav: WebDav): Long

  @Query(
    """
    SELECT * from WebDav ORDER BY createAt DESC
  """
  )
  fun selectAll() : Flow<List<WebDav>>

  @Query("UPDATE WebDav SET lastUrl = :url WHERE id = :id AND server = :server")
  suspend fun updateLastUrl(id: Int, server: String, url: String)

  @Delete
  suspend fun delete(webDav: WebDav): Int
}