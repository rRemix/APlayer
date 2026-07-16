package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.helper.AudioTagFile
import remix.myplayer.lyric.LrcParser
import remix.myplayer.util.ext.checkWorkerThread
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddedProvider @Inject constructor(
  @ApplicationContext
  context: Context
) : ILyricsProvider {

  override val id = LyricOrder.Embedded.toString()
  override val displayName = context.getString(LyricOrder.Embedded.stringRes)

  override suspend fun getLyrics(song: Song): LyricsResult {
    val lrc = extractLyric(song)
    if (lrc.isEmpty()) {
      throw Exception("Field `LYRICS` doesn't exist or is empty")
    }

    return LyricsResult(LrcParser.parse(lrc), id)
  }

  companion object {

    private val lrcTimestampPattern = Regex("""\[(\d+:){1,2}\d+(\.\d*)?]""")

    fun extractLyric(song: Song): String {
      checkWorkerThread()
      if (song !is Song.Local) {
        return ""
      }

      return runCatching {
        val propertyMap = AudioTagFile.readMetadata(File(song.data), readPictures = false)
          ?.propertyMap
          ?: return ""
        extractLyric(propertyMap)
      }.getOrDefault("")
    }

    private fun extractLyric(propertyMap: Map<String, Array<String>>): String {
      AudioTagFile.firstValue(propertyMap, AudioTagFile.LYRICS).takeIf(String::isNotEmpty)?.let {
        return it
      }

      // TagLib maps ID3 USLT descriptions to LYRICS:<description> and TXXX descriptions to
      // property keys, so this retains the former frame-specific fallback without ID3 APIs.
      propertyMap.entries.firstNotNullOfOrNull { (key, values) ->
        val normalizedKey = key.lowercase()
        values.firstOrNull { value ->
          normalizedKey.contains("lyric") ||
              normalizedKey.contains("lrc") ||
              normalizedKey.contains("歌词") ||
              lrcTimestampPattern.containsMatchIn(value)
        }
      }?.let { return it }

      return ""
    }
  }
}
