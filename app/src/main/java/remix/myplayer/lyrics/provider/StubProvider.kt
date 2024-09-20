package remix.myplayer.lyrics.provider

import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.LyricsLine

/**
 * 用于恢复默认
 */
object StubProvider : ILyricsProvider {
  override val id: String
    get() = throw RuntimeException()
  override val displayName: String
    get() = throw RuntimeException()

  override fun getLyrics(song: Song): List<LyricsLine> {
    throw RuntimeException()
  }
}
