package remix.myplayer.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.bean.misc.LyricOrder
import remix.myplayer.request.netease.NetEaseClient
import remix.myplayer.request.netease.NetEaseSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetEaseProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val netEaseClient: NetEaseClient
) : NetWorkLyricProvider<NetEaseSong>() {

  override val id = LyricOrder.Netease.toString()

  override val displayName = context.getString(LyricOrder.Netease.stringRes)

  override suspend fun searchSong(searchKey: String): CandidateSong<NetEaseSong>? {
    val neSong = netEaseClient.searchSong(searchKey) ?: return null
    return CandidateSong(
      raw = neSong,
      title = neSong.name,
      artist = neSong.ar?.joinToString(", ") { it.name ?: "" },
      album = neSong.al?.name,
      duration = neSong.dt
    )
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<NetEaseSong>): Pair<String?, String?> {
    return netEaseClient.getLyrics(candidateSong.raw)
  }
}