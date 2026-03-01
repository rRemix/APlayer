package remix.myplayer.lyric.provider

import android.content.Context
import android.net.Uri
import remix.myplayer.data.model.audio.Song
import remix.myplayer.lyric.LrcParser
import timber.log.Timber

class UriProvider(private val context: Context, private val uri: Uri) : ILyricsProvider {
  companion object {

    private const val TAG = "UriProvider"
  }

  override val id: String = "uri"
  override val displayName: String
    get() = throw RuntimeException() // 不应该用到

  override suspend fun getLyrics(song: Song): LyricsResult {
    return try {
      Timber.v("Try open uri: $uri")
      context.contentResolver.openInputStream(uri)!!.run {
        try {
          LyricsResult(LrcParser.parse(readBytes()), id)
        } catch (t: Throwable) {
          throw t
        } finally {
          close()
        }
      }
    } catch (t: Throwable) {
      Timber.tag(TAG).w(t, "Failed to get lyrics from URI: $uri")
      LyricsResult(emptyList(), id)
    }
  }
}
