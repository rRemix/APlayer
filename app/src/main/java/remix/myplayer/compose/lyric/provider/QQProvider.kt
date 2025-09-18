package remix.myplayer.compose.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.provider.ILyricsProvider.Companion.getLyricSearchKey
import remix.myplayer.request.network.QQApi
import remix.myplayer.util.Util
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QQProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val qqApi: QQApi) : ILyricsProvider {

  override val id = LyricOrder.Qq.toString()
  override val displayName = context.getString(R.string.qq)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    val searchKey = getLyricSearchKey(song)

    val searchResponse = qqApi.searchSong(1, searchKey, "json")
    if (song.title.equals(searchResponse.data.song.list[0].songname, true)) {
      val lyricResponse = qqApi.searchLyric(searchResponse.data.song.list[0].songmid, 5381, "json", 1)
      if (lyricResponse.lyric.isNotEmpty()) {
        val buffer = StringBuilder(Util.htmlToText(lyricResponse.lyric))
        if (lyricResponse.trans.isNotEmpty()) {
          buffer.append(Util.htmlToText(lyricResponse.trans))
        }
        return LrcParser.parse(buffer.toString())
      }
    }

    throw Exception("no lyric found by qq")
  }

}