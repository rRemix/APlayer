package remix.myplayer.compose.lyric.provider

import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LyricsLine

interface ILyricsProvider {

  val id: String
  val displayName: String

  /**
   * 返回的 List 为空不视为失败，仅抛出异常视为失败
   *
   * @throws Throwable
   */
  suspend fun getLyrics(song: Song): List<LyricsLine>

  companion object {
    // 目前只取最高优先级的
    const val CANDIDATE = 1
  }
}
