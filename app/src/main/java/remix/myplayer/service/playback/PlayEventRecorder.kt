package remix.myplayer.service.playback

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import remix.myplayer.data.db.room.entity.PlayEvent
import remix.myplayer.data.model.audio.PlayEventEndReason
import remix.myplayer.data.model.audio.PlayEventSource
import remix.myplayer.data.model.audio.Song
import remix.myplayer.repo.PlayEventRepository
import remix.myplayer.util.CanonicalIdProvider
import remix.myplayer.util.DeviceIdProvider
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * 在播放会话生命周期内累计“真实收听时长”，并在会话结束时写成一条 PlayEvent（schema v2）。
 *
 * 规则：
 * - begin：歌曲开始播放（或切到新歌且正在播放）时建立会话，并补登记 song_added（首次出现才写）。
 * - accumulate：仅在播放状态下，按 position 增量累计，增量异常视为 seek 忽略。
 * - end：切歌/自然结束/错误/停止/单曲循环重置时最终化。
 * - discard：有效收听比例 < 0.1（或未知时长时收听 < 1s）的会话不落库（但 song_added 已登记）。
 */
class PlayEventRecorder(
  private val repository: PlayEventRepository,
  private val deviceIdProvider: DeviceIdProvider,
  private val canonicalIdProvider: CanonicalIdProvider,
  private val scope: CoroutineScope,
  private val enabled: () -> Boolean,
) {

  private data class Session(
    val song: Song,
    val source: PlayEventSource,
    val startedAt: Long,
    val sessionId: String,
    val gapBeforeMs: Long?,
    var loopCount: Int,
    var lastPosition: Long = 0L,
    var listenedMs: Long = 0L,
  )

  private var currentSong: Song? = null
  private var session: Session? = null
  private var pendingSource: PlayEventSource? = null
  private var playing = false
  private var currentSessionId: String? = null
  private var lastEventEndedAt: Long = 0L

  /** 由播放入口（UI/Service）设置，本次“下一次开始播放”的来源。 */
  fun setPendingSource(source: PlayEventSource?) {
    pendingSource = source
  }

  fun onIsPlayingChanged(isPlaying: Boolean) {
    this.playing = isPlaying
    if (isPlaying) {
      val song = currentSong
      if (song != null && session?.song?.id != song.id) {
        begin(song)
      }
    }
  }

  fun onItemTransition(song: Song?, transitionReason: Int) {
    val current = session
    if (current != null && current.song.id != song?.id) {
      val reason = when (transitionReason) {
        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> PlayEventEndReason.NATURAL_END
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> PlayEventEndReason.PLAYLIST_CHANGED
        else -> PlayEventEndReason.SKIP_TO_NEXT
      }
      finishSession(reason)
    }
    currentSong = song
    if (playing && song != null) {
      begin(song)
    }
  }

  fun onPositionChange(position: Long) {
    val s = session ?: return
    if (!playing) return

    val delta = position - s.lastPosition
    if (delta >= 0 && delta <= MAX_SEEK_GAP) {
      s.listenedMs += delta
      s.lastPosition = position
    } else if (delta >= 0) {
      // seek 前进：不计入收听，但更新基准
      s.lastPosition = position
    } else {
      // 位置回退：若回到开头，视为单曲循环重新开始，单独记录新一轮
      if (position <= LOOP_RESET_THRESHOLD) {
        val prevLoop = s.loopCount
        finishSession(PlayEventEndReason.NATURAL_END)
        begin(song = currentSong ?: return, loopCount = prevLoop + 1)
      } else {
        s.lastPosition = position
      }
    }
  }

  fun onEnded() {
    finishSession(PlayEventEndReason.NATURAL_END)
  }

  fun onError() {
    finishSession(PlayEventEndReason.ERROR)
  }

  fun stop() {
    finishSession(PlayEventEndReason.STOP)
  }

  private fun begin(song: Song, loopCount: Int = 1) {
    if (!enabled()) return
    val startedAt = System.currentTimeMillis()
    val source = resolveSource()
    val sessionId = resolveSessionId()
    session = Session(
      song = song,
      source = source,
      startedAt = startedAt,
      sessionId = sessionId,
      gapBeforeMs = if (lastEventEndedAt > 0) (startedAt - lastEventEndedAt).coerceAtLeast(0) else null,
      loopCount = loopCount
    )
    maybeRecordSongAdded(song, sessionId, startedAt)
  }

  private fun resolveSource(): PlayEventSource {
    val pending = pendingSource
    return if (pending != null) {
      pendingSource = null
      pending
    } else {
      PlayEventSource.QUEUE_AUTO
    }
  }

  private fun resolveSessionId(): String {
    val now = System.currentTimeMillis()
    if (currentSessionId == null || (lastEventEndedAt > 0 && now - lastEventEndedAt > SESSION_GAP_MS)) {
      currentSessionId = "sess-" + UUID.randomUUID().toString().replace("-", "")
    }
    return currentSessionId ?: "sess-" + UUID.randomUUID().toString().replace("-", "")
  }

  private fun finishSession(endReason: PlayEventEndReason) {
    val s = session ?: return
    session = null
    if (!enabled()) return

    val endedAt = System.currentTimeMillis()
    val durationMs = s.song.duration.coerceAtLeast(0L)
    val ratio = if (durationMs > 0) {
      (s.listenedMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
    } else {
      0.0
    }
    val completed = durationMs > 0 &&
        (ratio >= COMPLETE_THRESHOLD || endReason == PlayEventEndReason.NATURAL_END)
    val isEffective = if (durationMs > 0) {
      ratio >= EFFECTIVE_THRESHOLD
    } else {
      s.listenedMs >= MIN_LISTEN_MS_UNKNOWN_DURATION
    }

    lastEventEndedAt = endedAt
    if (!isEffective || !s.song.valid()) return

    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = s.startedAt
    }
    val event = PlayEvent(
      schemaVersion = 2,
      eventId = "evt-" + UUID.randomUUID().toString().replace("-", ""),
      deviceId = deviceIdProvider.deviceId(),
      canonicalId = canonicalIdProvider.canonicalId(s.song),
      audioId = s.song.takeIf { it.isLocal() }?.id,
      startedAt = s.startedAt,
      endedAt = endedAt,
      durationMs = durationMs,
      listenedMs = s.listenedMs,
      listenRatio = ratio,
      playScore = ratio,
      completed = completed,
      source = s.source.name,
      endReason = endReason.name,
      titleSnapshot = s.song.title,
      artistSnapshot = s.song.artist,
      albumSnapshot = s.song.album,
      genreSnapshot = s.song.genre.takeIf { it.isNotBlank() },
      sourceUri = s.song.contentUri.toString(),
      contentHash = canonicalIdProvider.contentHash(s.song),
      pathHint = canonicalIdProvider.pathHint(s.song),
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
      hour = cal.get(Calendar.HOUR_OF_DAY),
      weekday = cal.get(Calendar.DAY_OF_WEEK),
      // v2
      songId = s.song.id.toString(),
      artistId = s.song.artistId.takeIf { it > 0 }?.toString(),
      albumId = s.song.albumId.takeIf { it > 0 }?.toString(),
      mediaType = songMediaType(s.song),
      sessionId = s.sessionId,
      gapBeforeMs = s.gapBeforeMs,
      loopCount = s.loopCount
    )

    scope.launch {
      repository.record(event)
    }
  }

  private fun maybeRecordSongAdded(song: Song, sessionId: String, addedAt: Long) {
    if (!enabled()) return
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = addedAt
    }
    val event = PlayEvent(
      schemaVersion = 2,
      eventId = "evt-" + UUID.randomUUID().toString().replace("-", ""),
      deviceId = deviceIdProvider.deviceId(),
      eventType = "song_added",
      canonicalId = canonicalIdProvider.canonicalId(song),
      audioId = song.takeIf { it.isLocal() }?.id,
      startedAt = addedAt,
      endedAt = addedAt,
      durationMs = song.duration.coerceAtLeast(0L),
      listenedMs = 0L,
      listenRatio = 0.0,
      playScore = 0.0,
      completed = false,
      source = "",
      endReason = "",
      titleSnapshot = song.title,
      artistSnapshot = song.artist,
      albumSnapshot = song.album,
      genreSnapshot = song.genre.takeIf { it.isNotBlank() },
      sourceUri = song.contentUri.toString(),
      contentHash = canonicalIdProvider.contentHash(song),
      pathHint = canonicalIdProvider.pathHint(song),
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
      hour = cal.get(Calendar.HOUR_OF_DAY),
      weekday = cal.get(Calendar.DAY_OF_WEEK),
      songId = song.id.toString(),
      artistId = song.artistId.takeIf { it > 0 }?.toString(),
      albumId = song.albumId.takeIf { it > 0 }?.toString(),
      mediaType = songMediaType(song),
      sessionId = sessionId,
      loopCount = 0
    )
    scope.launch {
      repository.recordSongAddedIfAbsent(event)
    }
  }

  private fun songMediaType(song: Song): String = when {
    song.isLocal() -> "local"
    song.contentUri.scheme == "smb" -> "smb"
    else -> "webdav"
  }

  companion object {
    private const val MAX_SEEK_GAP = 2500L
    private const val LOOP_RESET_THRESHOLD = 1000L
    private const val EFFECTIVE_THRESHOLD = 0.1
    private const val COMPLETE_THRESHOLD = 0.9
    private const val MIN_LISTEN_MS_UNKNOWN_DURATION = 1000L
    private const val SESSION_GAP_MS = 30L * 60L * 1000L
  }
}
