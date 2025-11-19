package remix.myplayer.lyric.provider.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.bean.misc.LyricOrder
import remix.myplayer.request.qq.QQClient
import remix.myplayer.request.qq.QQSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QQProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val qqClient: QQClient
) : NetworkProvider<QQSong>() {

  override val id = LyricOrder.Qq.toString()
  override val displayName = context.getString(LyricOrder.Qq.stringRes)

  override suspend fun searchSong(searchKey: String): CandidateSong<QQSong>? {
    val qqSong = qqClient.searchSong(searchKey) ?: return null
    return CandidateSong(
      raw = qqSong,
      title = qqSong.title,
      artist = qqSong.artist.joinToString(", "),
      album = qqSong.album,
      duration = qqSong.duration
    )
  }

  override suspend fun searchLyric(candidateSong: CandidateSong<QQSong>): Pair<String?, String?> {
    return qqClient.getLyrics(candidateSong.raw)
  }
}