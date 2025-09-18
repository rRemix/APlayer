package remix.myplayer.compose.lyric.provider

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.provider.ILyricsProvider.Companion.getLyricSearchKey
import remix.myplayer.request.network.KuGouApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuGouProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val kuGouApi: KuGouApi
) : ILyricsProvider {

  override val id = LyricOrder.Kugou.toString()

  override val displayName = context.getString(R.string.kugou)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    val searchKey = getLyricSearchKey(song)

    val searchResponse = kuGouApi.searchSong(1, "yes", "pc", searchKey, song.duration, "")
    if (searchResponse.candidates.isNotEmpty() &&
      song.title.equals(searchResponse.candidates[0].song, true)
    ) {
      val lyricResponse = kuGouApi.searchLyric(
        1, "pc", "lrc", "utf8",
        searchResponse.candidates[0].id,
        searchResponse.candidates[0].accesskey
      )

      val base64 = lyricResponse.content
      if (base64?.isNotEmpty() == true) {
        val data = String(Base64.decode(base64, Base64.DEFAULT))
        return LrcParser.parse(data)
      }
    }

    throw Exception("no lyric found by kugou")
  }
}