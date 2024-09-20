package remix.myplayer.lyrics.provider

import android.net.Uri
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.LrcParser
import remix.myplayer.lyrics.LyricsLine
import timber.log.Timber

class UriProvider(private val uri: Uri) : ILyricsProvider {
  companion object {
    private const val TAG = "UriProvider"
  }

  // 不应该用到
  override val id: String
    get() = throw RuntimeException()
  override val displayName: String
    get() = throw RuntimeException()

  override fun getLyrics(song: Song): List<LyricsLine> {
    return try {
      App.context.contentResolver.openInputStream(uri)!!.run {
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
