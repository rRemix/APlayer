package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.PlayQueue
import remix.myplayer.data.model.audio.SongIdentity

@Dao
interface PlayQueueDao {
  @Query("SELECT * FROM PlayQueue ORDER BY position")
  fun selectAll(): Flow<List<PlayQueue>>
  @Query("SELECT * FROM PlayQueue ORDER BY position")
  suspend fun get(): List<PlayQueue>
  @Query("DELETE FROM PlayQueue WHERE source_id = :source AND source_key = :key")
  suspend fun deleteSong(source: String, key: String): Int
  @Delete
  suspend fun delete(queues: List<PlayQueue>): Int
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(queue: List<PlayQueue>): LongArray
  @Query("DELETE FROM PlayQueue")
  suspend fun clear(): Int
  @Transaction
  suspend fun replace(queue: List<PlayQueue>) { clear(); insert(queue) }
  @Transaction
  suspend fun append(keys: List<SongIdentity>): LongArray {
    val existing = get()
    val oldKeys = existing.mapTo(hashSetOf()) { it.identity() }
    val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
    return insert(keys.distinct().filterNot { it in oldKeys }.mapIndexed { index, key ->
      PlayQueue(key.sourceId.value, key.sourceKey, position + index)
    })
  }
  @Transaction
  suspend fun remove(keys: List<SongIdentity>): Int =
    keys.distinct().sumOf { deleteSong(it.sourceId.value, it.sourceKey) }
}
