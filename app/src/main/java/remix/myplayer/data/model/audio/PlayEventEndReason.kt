package remix.myplayer.data.model.audio

/**
 * 播放事件结束原因。
 */
enum class PlayEventEndReason(val value: Int) {
  NATURAL_END(0),
  SKIP_TO_NEXT(1),
  SKIP_TO_PREVIOUS(2),
  PLAYLIST_CHANGED(3),
  STOP(4),
  ERROR(5);

  companion object {
    fun fromValue(value: Int): PlayEventEndReason? = entries.firstOrNull { it.value == value }
  }
}
