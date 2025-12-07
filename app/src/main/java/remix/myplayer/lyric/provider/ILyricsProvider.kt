package remix.myplayer.lyric.provider

import remix.myplayer.data.model.audio.Song
import remix.myplayer.lyric.LyricLine

interface ILyricsProvider {

  val id: String
  val displayName: String

  /**
   * 返回的 List 为空不视为失败，仅抛出异常视为失败
   *
   * @throws Throwable
   */
  suspend fun getLyrics(song: Song): LyricsResult

  companion object {
    // 目前只取最高优先级的
    const val CANDIDATE_KEY_NUMBER = 1
  }
}

data class LyricsResult(
  val data: List<LyricLine>,
  val providerId: String
)