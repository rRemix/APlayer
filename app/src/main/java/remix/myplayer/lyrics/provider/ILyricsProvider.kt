package remix.myplayer.lyrics.provider

import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.LyricsLine

interface ILyricsProvider {
  val id: String
  val displayName: String

  /**
   * 返回的 List 为空不视为失败，仅抛出异常视为失败
   *
   * @throws Throwable
   */
  fun getLyrics(song: Song): List<LyricsLine>
}
