package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.data.bean.misc.LyricOrder
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.lyric.LrcParser
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
    if (song is Song.Local) {
      val lrc = AudioFileIO.read(File(song.data)).tag.getFirst(FieldKey.LYRICS)
      if (lrc.isNullOrEmpty()) {
        throw Exception("Field `LYRICS` doesn't exist or is empty")
      }
      return LyricsResult(LrcParser.parse(lrc), id)
    }

    throw Exception("no lyric found by $id")
  }
}
