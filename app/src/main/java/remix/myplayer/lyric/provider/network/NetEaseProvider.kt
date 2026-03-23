package remix.myplayer.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.request.netease.NetEaseClient
import remix.myplayer.request.netease.NetEaseSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetEaseProvider @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val netEaseClient: NetEaseClient
) : NetworkProvider<NetEaseSong>() {

  override val id = LyricOrder.Netease.toString()

  override val displayName = context.getString(LyricOrder.Netease.stringRes)

  override suspend fun searchCandidates(searchKey: String): List<CandidateSong<NetEaseSong>> {
    val list = netEaseClient.searchSongList(searchKey)
    return list.map { neSong ->
      CandidateSong(
        raw = neSong,
        title = neSong.name,
        artist = neSong.ar?.joinToString(", ") { it.name ?: "" },
        album = neSong.al?.name,
        duration = neSong.dt
      )
    }
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<NetEaseSong>): Pair<String?, String?> {
    return netEaseClient.getLyrics(candidateSong.raw)
  }
}
