package remix.myplayer.lyric.provider.network

import remix.myplayer.data.model.audio.Song
import remix.myplayer.lyric.LrcParser
import remix.myplayer.lyric.provider.ILyricsProvider
import remix.myplayer.lyric.provider.ILyricsProvider.Companion.CANDIDATE_KEY_NUMBER
import remix.myplayer.lyric.provider.LyricsResult
import remix.myplayer.lyric.provider.SearchScorer
import remix.myplayer.util.SearchKeyUtil.getSearchKeys

abstract class NetworkProvider<T> : ILyricsProvider {

  protected data class CandidateSong<T>(
    val raw: T,
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: Long?
  )

  final override suspend fun getLyrics(song: Song): LyricsResult {
    val searchKeys = getSearchKeys(song)

    for (key in searchKeys.take(CANDIDATE_KEY_NUMBER)) {
      val candidates = searchCandidates(key.value)
      if (candidates.isEmpty()) {
        continue
      }
      val best = candidates
        .map {
          it to SearchScorer.calculateSongScoreWithKeyKind(
            song,
            it.title,
            it.artist,
            it.album,
            it.duration,
            key.value,
            key.kind
          )
        }
        .filter { it.second.isValid }
        .maxByOrNull { it.second.score }
        ?.first

      if (best != null) {
        val (lyric, tlyric) = searchLyric(best)
        if (!lyric.isNullOrEmpty()) {
          val combined = if (!tlyric.isNullOrEmpty()) {
            lyric.trimEnd() + "\n" + tlyric.trimStart()
          } else {
            lyric
          }
          return LyricsResult(LrcParser.parse(combined), id)
        }
      }
    }

    throw Exception("no lyric found by $id")
  }

  protected abstract suspend fun searchCandidates(searchKey: String): List<CandidateSong<T>>

  protected abstract suspend fun searchLyric(candidateSong: CandidateSong<T>): Pair<String?, String?>
}