package remix.myplayer.data.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy.Companion.REPLACE
import remix.myplayer.data.db.room.entity.PlayEvent

@Dao
interface PlayEventDao {

  @Insert(onConflict = REPLACE)
  suspend fun insert(event: PlayEvent): Long

  // 导出用：包税全部事件类型（playback + song_added）
  @Query("SELECT * FROM play_events WHERE year = :year ORDER BY startedAt DESC")
  suspend fun selectByYear(year: Int): List<PlayEvent>

  // 报告年份选择：只统计播放事件
  @Query("SELECT DISTINCT year FROM play_events WHERE eventType = 'playback' ORDER BY year DESC")
  suspend fun distinctYears(): List<Int>

  // ---- 播放类聚合（只统计 playback）----
  @Query("SELECT COUNT(*) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countByYear(year: Int): Int

  @Query("SELECT COUNT(*) FROM play_events WHERE eventType = 'song_added' AND canonicalId = :canonicalId")
  suspend fun countSongAdded(canonicalId: String): Int

  @Query("SELECT COUNT(*) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countPlayable(year: Int): Int

  @Query("SELECT COALESCE(SUM(playScore), 0) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun sumPlayScore(year: Int): Double

  @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun sumListenMs(year: Int): Long

  @Query("SELECT COUNT(*) FROM play_events WHERE eventType = 'playback' AND year = :year AND completed = 1")
  suspend fun countCompleted(year: Int): Int

  @Query("SELECT COUNT(DISTINCT day) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countListenDays(year: Int): Int

  @Query("SELECT COUNT(DISTINCT canonicalId) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countDistinctSongs(year: Int): Int

  @Query("SELECT COUNT(DISTINCT artistSnapshot) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countDistinctArtists(year: Int): Int

  @Query("SELECT COUNT(DISTINCT albumSnapshot) FROM play_events WHERE eventType = 'playback' AND year = :year")
  suspend fun countDistinctAlbums(year: Int): Int

  // 当年首次收听的歌曲数（该歌曲最早一次播放发生在该年）
  @Query(
    """
    SELECT COUNT(*) FROM (
      SELECT canonicalId, MIN(startedAt) AS firstAt
      FROM play_events
      WHERE eventType = 'playback'
      GROUP BY canonicalId
      HAVING CAST(strftime('%Y', firstAt / 1000, 'unixepoch') AS INTEGER) = :year
    )
    """
  )
  suspend fun countFirstListenedSongs(year: Int): Int

  // 当年新增歌曲数（song_added 且 year = 该年）
  @Query("SELECT COUNT(*) FROM play_events WHERE eventType = 'song_added' AND year = :year")
  suspend fun countAddedSongs(year: Int): Int

  @Query(
    """
    SELECT canonicalId AS canonicalId,
      (SELECT titleSnapshot FROM play_events e2 WHERE e2.canonicalId = e1.canonicalId AND e2.eventType = 'playback' AND e2.year = :year ORDER BY e2.startedAt DESC LIMIT 1) AS title,
      (SELECT artistSnapshot FROM play_events e2 WHERE e2.canonicalId = e1.canonicalId AND e2.eventType = 'playback' AND e2.year = :year ORDER BY e2.startedAt DESC LIMIT 1) AS artist,
      (SELECT albumSnapshot FROM play_events e2 WHERE e2.canonicalId = e1.canonicalId AND e2.eventType = 'playback' AND e2.year = :year ORDER BY e2.startedAt DESC LIMIT 1) AS album,
      SUM(listenedMs) AS listenedMs,
      SUM(playScore) AS playScore,
      COUNT(*) AS plays
    FROM play_events e1
    WHERE e1.eventType = 'playback' AND e1.year = :year
    GROUP BY canonicalId
    ORDER BY listenedMs DESC
    LIMIT :limit
    """
  )
  suspend fun topSongs(year: Int, limit: Int): List<TopPlayItem>

  @Query(
    """
    SELECT artistSnapshot AS name,
      SUM(listenedMs) AS listenedMs,
      COUNT(*) AS plays,
      COUNT(DISTINCT canonicalId) AS songs
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
    GROUP BY artistSnapshot
    ORDER BY listenedMs DESC
    LIMIT :limit
    """
  )
  suspend fun topArtists(year: Int, limit: Int): List<TopArtistItem>

  @Query(
    """
    SELECT albumSnapshot AS name,
      SUM(listenedMs) AS listenedMs,
      COUNT(*) AS plays,
      COUNT(DISTINCT canonicalId) AS songs
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
    GROUP BY albumSnapshot
    ORDER BY listenedMs DESC
    LIMIT :limit
    """
  )
  suspend fun topAlbums(year: Int, limit: Int): List<TopAlbumItem>

  @Query(
    """
    SELECT month AS month, COUNT(*) AS plays, COALESCE(SUM(listenedMs), 0) AS listenedMs
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
    GROUP BY month
    ORDER BY month
    """
  )
  suspend fun monthDistribution(year: Int): List<MonthCount>

  @Query(
    """
    SELECT hour AS hour, COUNT(*) AS plays
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
    GROUP BY hour
    ORDER BY hour
    """
  )
  suspend fun hourDistribution(year: Int): List<HourCount>

  @Query(
    """
    SELECT source AS source, COUNT(*) AS plays, COALESCE(SUM(listenedMs), 0) AS listenedMs
    FROM play_events
    WHERE eventType = 'playback' AND year = :year
    GROUP BY source
    ORDER BY plays DESC
    """
  )
  suspend fun sourceBreakdown(year: Int): List<SourceCount>

  @Query("SELECT COUNT(*) FROM play_events")
  suspend fun countAll(): Int

  @Query("DELETE FROM play_events")
  suspend fun clear()

  @Query("DELETE FROM play_events WHERE year = :year")
  suspend fun deleteByYear(year: Int)
}

data class TopPlayItem(
  val canonicalId: String,
  val title: String,
  val artist: String,
  val album: String,
  val listenedMs: Long,
  val playScore: Double,
  val plays: Int
)

data class TopArtistItem(
  val name: String,
  val listenedMs: Long,
  val plays: Int,
  val songs: Int
)

data class TopAlbumItem(
  val name: String,
  val listenedMs: Long,
  val plays: Int,
  val songs: Int
)

data class MonthCount(
  val month: Int,
  val plays: Int,
  val listenedMs: Long
)

data class HourCount(
  val hour: Int,
  val plays: Int
)

data class SourceCount(
  val source: String,
  val plays: Int,
  val listenedMs: Long
)
