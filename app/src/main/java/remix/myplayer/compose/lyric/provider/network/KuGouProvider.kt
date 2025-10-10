package remix.myplayer.compose.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.request.kugou.KuGouClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuGouProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val kuGouClient: KuGouClient
) : NetWorkLyricProvider<KuGouClient.KuGouSong>() {

  override val id = LyricOrder.Kugou.toString()

  override val displayName = context.getString(LyricOrder.Kugou.stringRes)

  override suspend fun searchSong(searchKey: String): CandidateSong<KuGouClient.KuGouSong>? {
    val kgSong = kuGouClient.searchSong(searchKey) ?: return null
    return CandidateSong(
      raw = kgSong,
      title = kgSong.title,
      artist = kgSong.artists.joinToString(", "),
      album = kgSong.album,
      duration = kgSong.durationMs
    )
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<KuGouClient.KuGouSong>): Pair<String?, String?> {
    return kuGouClient.getLyrics(candidateSong.raw)
  }
}