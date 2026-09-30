package remix.myplayer.data.model.report

import kotlinx.serialization.Serializable

/**
 * JSONL 导出协议数据（schema v2）。时间统一使用 UTC ISO 8601。
 * v2 字段全部可选，缺省不出现（跨端读取需容忍缺省）。
 */
@Serializable
data class PlayEventExport(
  val schemaVersion: Int,
  val eventId: String,
  val deviceId: String,
  val eventType: String,
  val startedAt: String,
  val endedAt: String,
  val durationMs: Long,
  val listenedMs: Long,
  val listenRatio: Double,
  val playScore: Double,
  val completed: Boolean,
  val source: String,
  val endReason: String,
  val track: TrackExport,
  // ---- v2 可选 ----
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

@Serializable
data class TrackExport(
  val canonicalId: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long
)
