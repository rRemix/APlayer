package remix.myplayer.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.request.qq.QQClient
import remix.myplayer.request.qq.QQSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QQProvider @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val qqClient: QQClient
) : NetworkProvider<QQSong>() {

  override val id = LyricOrder.Qq.toString()
  override val displayName = context.getString(LyricOrder.Qq.stringRes)

  override suspend fun searchCandidates(searchKey: String): List<CandidateSong<QQSong>> {
    val list = qqClient.searchSongList(searchKey)
    return list.map { s ->
      CandidateSong(
        raw = s,
        title = s.title,
        artist = s.artist.joinToString(", "),
        album = s.album,
        duration = s.duration
      )
    }
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<QQSong>): Pair<String?, String?> {
    return qqClient.getLyrics(candidateSong.raw)
  }
}
