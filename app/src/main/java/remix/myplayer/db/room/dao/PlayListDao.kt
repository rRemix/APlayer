package remix.myplayer.db.room.dao

import androidx.room.*
import remix.myplayer.db.room.model.PlayList
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery



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

  @RawQuery
  fun runtimeQuery(sortQuery: SupportSQLiteQuery): List<PlayList>


  @Query("""
    SELECT * FROM PlayList
    WHERE name = :name
  """)
  fun selectByName(name: String): PlayList?

  @Query("""
    SELECT * FROM PlayList
    WHERE id = :id
  """)
  fun selectById(id: Long): PlayList?

  @Query("""
    UPDATE PlayList
    SET audioIds = :audioIds
    WHERE id = :playlistId
  """)
  fun updateAudioIDs(playlistId: Long, audioIds: String): Int

  @Query("""
    UPDATE PlayList
    SET audioIds = :audioIds
    WHERE id = :name
  """)
  fun updateAudioIDs(name: String, audioIds: String): Int


  @Update
  fun update(playlist: PlayList): Int

  @Query("""
    DELETE FROM PlayList
    WHERE id = :id
  """)
  fun deletePlayList(id: Long): Int

  @Query("""
    DELETE FROM PlayList
  """)
  fun clear(): Int

}