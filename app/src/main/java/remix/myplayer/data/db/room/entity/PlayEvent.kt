package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单次播放事件（v2）。
 *
 * 字段遵循跨平台协议：eventId/deviceId/schemaVersion/canonicalId 与平台无关。
 * startedAt/endedAt 使用 UTC epoch 毫秒。listenRatio/playScore/completed 为派生值。
 * v2 新增的可选字段用于更丰富的年度统计（媒体类型/稳定ID/会话/循环/环境占位）。
 */
@Entity(
  tableName = "play_events",
  indices = [
    Index(value = ["year"]),
    Index(value = ["canonicalId"]),
    Index(value = ["audioId"]),
    Index(value = ["source"]),
    Index(value = ["startedAt"]),
    Index(value = ["eventType"])
  ]
)
data class PlayEvent(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val schemaVersion: Int = 2,
  val eventId: String,
  val deviceId: String,
  val eventType: String = "playback",
  val canonicalId: String,
  val audioId: Long?,
  val startedAt: Long,
  val endedAt: Long,
  val durationMs: Long,
  val listenedMs: Long,
  val listenRatio: Double,
  val playScore: Double,
  val completed: Boolean,
  val source: String,
  val endReason: String,
  val titleSnapshot: String,
  val artistSnapshot: String,
  val albumSnapshot: String,
  val genreSnapshot: String?,
  val sourceUri: String?,
  val contentHash: String?,
  val pathHint: String?,
  val year: Int,
  val month: Int,
  val day: Int,
  val hour: Int,
  val weekday: Int,
  // ---- v2 扩展（全部可选，向前兼容） ----
  val songId: String? = null,
  val artistId: String? = null,
  val albumId: String? = null,
  val genreId: String? = null,
  val playlistId: String? = null,
  val mediaType: String? = null,
  val sessionId: String? = null,
  val gapBeforeMs: Long? = null,
  val gapAfterMs: Long? = null,
  val loopCount: Int = 0,
  val outputDevice: String? = null,
  val isForeground: Boolean? = null,
  val decoder: String? = null
)
