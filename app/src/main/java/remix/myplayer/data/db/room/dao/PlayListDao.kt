package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.db.room.entity.PlayListSong
import remix.myplayer.data.db.room.entity.PlayListWithSongs
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId

@Dao
interface PlayListDao {
  @Query("SELECT EXISTS(SELECT * FROM PlayList WHERE name = :name)")
  suspend fun exists(name: String): Boolean
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(playlist: PlayList): Long
  @Query("SELECT * FROM PlayList WHERE name = :name LIMIT 1")
  suspend fun selectByName(name: String): PlayList?
  @Transaction
  @Query("SELECT * FROM PlayList WHERE id = :id LIMIT 1")
  suspend fun selectById(id: Long): PlayListWithSongs?
  @Transaction
  @Query("SELECT * FROM PlayList WHERE id = :id LIMIT 1")
  fun observeById(id: Long): Flow<PlayListWithSongs?>
  @Transaction
  @Query("SELECT * FROM PlayList")
  fun selectAll(): Flow<List<PlayListWithSongs>>
  @Transaction
  @Query("SELECT * FROM PlayList")
  suspend fun selectAllOnce(): List<PlayListWithSongs>
  @Query("SELECT DISTINCT source_key FROM playlist_tracks WHERE source_id = 'mediastore:external'")
  suspend fun localKeys(): List<String>
  @Query("UPDATE PlayList SET name = :name WHERE id = :id")
  suspend fun rename(id: Long, name: String): Int
  @Query("DELETE FROM PlayList WHERE id = :id")
  suspend fun delete(id: Long): Int
  @Query("DELETE FROM PlayList")
  suspend fun clear(): Int
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertSong(song: PlayListSong): Long
  @Query("SELECT * FROM playlist_tracks WHERE playlist_id = :id ORDER BY position")
  suspend fun tracks(id: Long): List<PlayListSong>
  @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlist_id = :id")
  suspend fun maxPosition(id: Long): Int
  @Query("DELETE FROM playlist_tracks WHERE playlist_id = :id")
  suspend fun clearTracks(id: Long): Int
  @Query("DELETE FROM playlist_tracks WHERE playlist_id = :id AND source_id = :source AND source_key = :key")
  suspend fun removeTrack(id: Long, source: String, key: String): Int
  @Query("DELETE FROM playlist_tracks WHERE source_id = :source AND source_key = :key")
  suspend fun removeTrackFromAll(source: String, key: String): Int
  @Query("SELECT EXISTS(SELECT * FROM playlist_tracks WHERE playlist_id = :id AND source_id = :source AND source_key = :key)")
  suspend fun contains(id: Long, source: String, key: String): Boolean

  @Transaction
  suspend fun addSongs(id: Long, keys: List<SongIdentity>): Int {
    var position = maxPosition(id) + 1
    var count = 0
    keys.distinct().forEach { key ->
      if (insertSong(PlayListSong(id, key.sourceId.value, key.sourceKey, position)) != -1L) {
        count++
        position++
      }
    }
    return count
  }

  /** Reorder only loaded rows; an unavailable source must retain its saved position. */
  @Transaction
  suspend fun updateSongOrder(id: Long, keys: List<SongIdentity>) {
    val rows = tracks(id)
    val byKey = rows.associateBy { SongIdentity(SourceId(it.sourceId), it.sourceKey) }
    val ordered = keys.distinct().mapNotNull(byKey::get)
    val included = ordered.mapTo(hashSetOf()) { it.sourceId to it.sourceKey }
    val iterator = ordered.iterator()
    val result = rows.map { if ((it.sourceId to it.sourceKey) in included) iterator.next() else it }
    if (result == rows) return
    clearTracks(id)
    result.forEachIndexed { index, row -> insertSong(row.copy(position = index)) }
  }

  @Transaction
  suspend fun removeSongs(id: Long, keys: Set<SongIdentity>): Int =
    keys.sumOf { removeTrack(id, it.sourceId.value, it.sourceKey) }

  @Transaction
  suspend fun removeSongsFromAll(keys: Set<SongIdentity>): Int =
    keys.sumOf { removeTrackFromAll(it.sourceId.value, it.sourceKey) }

  @Transaction
  suspend fun toggle(id: Long, key: SongIdentity): Boolean {
    if (contains(id, key.sourceId.value, key.sourceKey)) {
      removeTrack(id, key.sourceId.value, key.sourceKey)
      return false
    }
    return addSongs(id, listOf(key)) > 0
  }
}
