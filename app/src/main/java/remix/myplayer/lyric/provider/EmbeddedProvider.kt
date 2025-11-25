package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
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
      val audioFile = AudioFileIO.read(File(song.data))

      // 先读标准的 FieldKey.LYRICS
      var lrc = audioFile.tag.getFirst(FieldKey.LYRICS)
      if (lrc.isNullOrEmpty()) {
        val uslt = audioFile.tag.getFirst("USLT")
        if (!uslt.isNullOrEmpty()) {
          lrc = uslt
        }
      }

      // 如果没有，再尝试扫描所有 TXXX
      if (lrc.isNullOrEmpty()) {
        val candidates = audioFile.tag.getFields("TXXX")
        for (f in candidates) {
          if (f is AbstractID3v2Frame) {
            val body = f.body
            if (body is FrameBodyTXXX) {
              val desc = body.description?.lowercase()?.trim()
              val text = body.text
              if ((desc != null &&
                    (desc.contains("lyric") || desc.contains("lrc") || desc.contains("歌词")))
                || text.contains(Regex("""\[(\d+:){1,2}\d+(\.\d*)?]"""))
              ) {
                lrc = text
                break
              }
            }
          }
        }
      }

      if (lrc.isNullOrEmpty()) {
        throw Exception("Field `LYRICS` doesn't exist or is empty")
      }
      return LyricsResult(LrcParser.parse(lrc), id)
    }

    throw Exception("no lyric found by $id")
  }
}
