package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import remix.myplayer.data.db.room.entity.MetaDataCache

@Dao
interface MetaDataCacheDao {

  @Query("SELECT * FROM MetaDataCache WHERE url = :url")
  suspend fun get(url: String): MetaDataCache?

  @Query("SELECT * FROM MetaDataCache WHERE url IN (:urls)")
  suspend fun getAll(urls: List<String>): List<MetaDataCache>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(cache: MetaDataCache)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(caches: List<MetaDataCache>)

  @Query("DELETE FROM MetaDataCache WHERE url = :key")
  suspend fun delete(key: String)
}