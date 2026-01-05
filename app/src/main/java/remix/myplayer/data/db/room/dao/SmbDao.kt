package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.Smb

@Dao
interface SmbDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(smb: Smb): Long

  @Query(
    """
    SELECT * from Smb ORDER BY createAt DESC
  """
  )
  fun selectAll() : Flow<List<Smb>>

  @Delete
  suspend fun delete(smb: Smb): Int
}
