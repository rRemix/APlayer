package remix.myplayer.data.model.audio

/**
 * 播放来源：描述一次播放是如何被触发的。
 */
enum class PlayEventSource(val value: Int) {
  SEARCH_CLICK(0),
  LIBRARY_CLICK(1),
  PLAYLIST_CLICK(2),
  QUEUE_AUTO(3),
  RESUME(4),
  EXTERNAL_INTENT(5);

  companion object {
    fun fromValue(value: Int): PlayEventSource? = entries.firstOrNull { it.value == value }

    fun fromName(name: String?): PlayEventSource? = entries.firstOrNull { it.name == name }
  }
}
