package remix.myplayer.data.model.report

import remix.myplayer.data.db.room.dao.HourCount
import remix.myplayer.data.db.room.dao.MonthCount
import remix.myplayer.data.db.room.dao.SourceCount
import remix.myplayer.data.db.room.dao.TopAlbumItem
import remix.myplayer.data.db.room.dao.TopArtistItem
import remix.myplayer.data.db.room.dao.TopPlayItem

/**
 * 某一年度的听歌报告聚合结果。
 */
data class AnnualReport(
  val year: Int,
  val plays: Int,
  val listenScore: Double,
  val completedPlays: Int,
  val listenMs: Long,
  val listenedDays: Int,
  val distinctSongs: Int,
  val distinctArtists: Int,
  val distinctAlbums: Int,
  val firstListenedSongs: Int,
  val addedSongs: Int,
  val topSongs: List<TopPlayItem>,
  val topArtists: List<TopArtistItem>,
  val topAlbums: List<TopAlbumItem>,
  val monthDistribution: List<MonthCount>,
  val hourDistribution: List<HourCount>,
  val sourceBreakdown: List<SourceCount>
)

/**
 * 排行榜单项。
 */
data class TopItem(
  val name: String,
  val listenedMs: Long,
  val plays: Int
)
