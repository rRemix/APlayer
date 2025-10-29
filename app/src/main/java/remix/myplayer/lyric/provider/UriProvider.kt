package remix.myplayer.lyric.provider

import android.content.Context
import android.net.Uri
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.lyric.LrcParser
import remix.myplayer.lyric.LyricsLine
import timber.log.Timber

class UriProvider(private val context: Context, private val uri: Uri) : ILyricsProvider {
  companion object {

    private const val TAG = "UriProvider"
  }

  override val id: String = "uri"
  override val displayName: String
    get() = throw RuntimeException() // 不应该用到

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    return try {
      context.contentResolver.openInputStream(uri)!!.run {
        try {
          LrcParser.parse(readBytes().decodeToString())
        } catch (t: Throwable) {
          throw t
        } finally {
          close()
        }
      }
    } catch (t: Throwable) {
      Timber.tag(TAG).w(t, "Failed to get lyrics from URI: $uri")
      emptyList()
    }
  }
}
