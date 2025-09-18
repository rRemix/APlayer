package remix.myplayer.compose.lyric.provider

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LrcParser
import remix.myplayer.compose.lyric.LyricsLine
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFileProvider @Inject constructor(
  @ApplicationContext
  private val context: Context,
) : ILyricsProvider {

  override val id = LyricOrder.Local.toString()

  override val displayName = context.getString(R.string.local)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    val path = getLocalLyricPath(song)
    if (path != null && path.isNotEmpty()) {
      return LrcParser.parse(path.reader().readText())
    }

    throw Exception("no lyric found by local file")
  }

  private fun getLocalLyricPath(song: Song): String? {
    var path = ""
    //没有设置歌词路径 搜索所有可能的歌词文件
    context.contentResolver.query(
      MediaStore.Files.getContentUri("external"), null,
      MediaStore.Files.FileColumns.DATA + " like ? or " +
          MediaStore.Files.FileColumns.DATA + " like ? or " +
          MediaStore.Files.FileColumns.DATA + " like ? or " +
          MediaStore.Files.FileColumns.DATA + " like ? or " +
          MediaStore.Files.FileColumns.DATA + " like ? or " +
          MediaStore.Files.FileColumns.DATA + " like ?",
      getLocalSearchKey(song),
      null
    )
      .use { filesCursor ->
        if (filesCursor == null) {
          return ""
        }
        while (filesCursor.moveToNext()) {
          val file =
            File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
          Timber.v("file: %s", file.absolutePath)
          if (file.exists() && file.isFile && file.canRead()) {
            path = file.absolutePath
            break
          }
        }
        return path
      }
  }

  /**
   * @param searchPath 设置的本地歌词搜索路径
   * 本地歌词搜索的关键字
   * artist-displayName.lrc
   * displayName.lrc
   * title.lrc
   * title-artist.lrc
   * displayname-artist.lrc
   * artist-title.lrc
   */
  private fun getLocalSearchKey(song: Song, searchPath: String? = null): Array<String> {
    return arrayOf(
      "%${song.artist}%$displayName$SUFFIX_LYRIC",
      "%$displayName$SUFFIX_LYRIC",
      "%${song.title}$SUFFIX_LYRIC",
      "%${song.title}%${song.artist}$SUFFIX_LYRIC",
      "%${song.displayName}%${song.artist}$SUFFIX_LYRIC",
      "%${song.artist}%${song.title}$SUFFIX_LYRIC"
    )
  }

  companion object {

    private const val SUFFIX_LYRIC = ".lrc"
  }
}