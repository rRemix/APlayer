package remix.myplayer.compose.lyric.provider.network

import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.provider.ILyricsProvider
import remix.myplayer.compose.lyric.provider.ILyricsProvider.Companion.CANDIDATE
import remix.myplayer.compose.lyric.provider.SearchScorer
import remix.myplayer.util.SearchKeyUtil.getSearchKeys

abstract class NetWorkLyricProvider<T> : ILyricsProvider {

  protected data class CandidateSong<T>(
    val raw: T,
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: Long?
  )

  final override suspend fun getLyrics(song: Song): List<LyricsLine> {
    val searchKeys = getSearchKeys(song)

    for (key in searchKeys.take(CANDIDATE)) {
      val candidateSong = searchSong(key) ?: continue

      val scoreResult = SearchScorer.calculateScore(
        targetSong = song,
        candidateTitle = candidateSong.title,
        candidateArtist = candidateSong.artist,
        candidateAlbum = candidateSong.album,
        candidateDuration = candidateSong.duration
      )

      if (scoreResult.isValid) {
        val (lyric, tlyric) = searchLyric(candidateSong)
        if (!lyric.isNullOrEmpty()) {
          val combined = if (!tlyric.isNullOrEmpty()) {
            lyric.trimEnd() + "\n" + tlyric.trimStart()
          } else {
            lyric
          }
          return LrcParser.parse(combined)
        }
      }
    }

    throw Exception("no lyric found by $id")
  }

  protected abstract suspend fun searchSong(searchKey: String): CandidateSong<T>?

  protected abstract suspend fun searchLyric(candidateSong: CandidateSong<T>): Pair<String?, String?>
}