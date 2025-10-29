package remix.myplayer.lyric.provider

import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.lyric.LyricsLine

/**
 * 用于恢复默认
 */
object StubProvider : ILyricsProvider {
  override val id: String
    get() = throw RuntimeException()
  override val displayName: String
    get() = throw RuntimeException()

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    throw RuntimeException()
  }
}
