package remix.myplayer.lyrics.provider

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.LrcParser
import remix.myplayer.lyrics.LyricsLine
import java.io.File

object EmbeddedProvider : ILyricsProvider {
  override val id = "embedded"
  override val displayName by lazy {
    App.context.getString(R.string.embedded_lyric)
  }

  override fun getLyrics(song: Song): List<LyricsLine> {
    if (song is Song.Local) {
      val lrc = AudioFileIO.read(File(song.data)).tag.getFirst(FieldKey.LYRICS)
      if (lrc.isNullOrEmpty()) {
        throw Exception("Field `LYRICS` doesn't exist or is empty")
      }
      return LrcParser.parse(lrc)
    }
    TODO("Reading embedded lyrics of song type ${song.javaClass.simpleName} is not supported yet")
  }
}
