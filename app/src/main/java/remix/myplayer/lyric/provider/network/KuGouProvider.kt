package remix.myplayer.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.request.kugou.KuGouClient
import remix.myplayer.request.kugou.KuGouSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuGouProvider @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val kuGouClient: KuGouClient
) : NetworkProvider<KuGouSong>() {

  override val id = LyricOrder.Kugou.toString()

  override val displayName = context.getString(LyricOrder.Kugou.stringRes)

  override suspend fun searchCandidates(searchKey: String): List<CandidateSong<KuGouSong>> {
    val list = kuGouClient.searchSongList(searchKey)
    return list.map { s ->
      CandidateSong(
        raw = s,
        title = s.title,
        artist = s.artists.joinToString(", "),
        album = s.album,
        duration = s.durationMs
      )
    }
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<KuGouSong>): Pair<String?, String?> {
    return kuGouClient.getLyrics(candidateSong.raw)
  }
}
