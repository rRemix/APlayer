package remix.myplayer.compose.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.provider.ILyricsProvider.Companion.getLyricSearchKey
import remix.myplayer.request.network.NetEaseApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetEaseProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val netEaseApi: NetEaseApi
) : ILyricsProvider {

  override val id = LyricOrder.Netease.toString()

  override val displayName = context.getString(R.string.netease)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    val searchKey = getLyricSearchKey(song)

    val searchResponse = netEaseApi.searchSong(searchKey, 0, 1, 1)
    val song = searchResponse.result?.songs?.getOrNull(0)
    if (song != null) {
      val lyricResponse = netEaseApi.searchLyric("pc", song.id, -1, -1, -1)
      if (lyricResponse.lrc?.lyric?.isNotEmpty() == true) {
        val buffer = StringBuilder(lyricResponse.lrc.lyric)
        if (lyricResponse.tlyric?.lyric?.isNotEmpty() == true) {
          buffer.append(lyricResponse.tlyric.lyric)
        }
        return LrcParser.parse(buffer.toString())
      }
    }

    throw Exception("no lyric found by netease")
  }
}