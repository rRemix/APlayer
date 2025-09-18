package remix.myplayer.compose.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddedProvider @Inject constructor(
  @ApplicationContext
  context: Context
) : ILyricsProvider {

  override val id = LyricOrder.Embedded.toString()
  override val displayName by lazy {
    context.getString(R.string.embedded_lyric)
  }

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    if (song is Song.Local) {
      val lrc = AudioFileIO.read(File(song.data)).tag.getFirst(FieldKey.LYRICS)
      if (lrc.isNullOrEmpty()) {
        throw Exception("Field `LYRICS` doesn't exist or is empty")
      }
      return LrcParser.parse(lrc)
    }

    throw Exception("no lyric found by qq")
  }
}
