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

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(cache: MetaDataCache)

  @Query("DELETE FROM MetaDataCache WHERE updateTime < :timestamp")
  suspend fun deleteOldCache(timestamp: Long)
}