package remix.myplayer.lyric.provider

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.lyric.LrcParser
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFileProvider @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
) : ILyricsProvider {

  override val id = LyricOrder.Local.toString()

  override val displayName = context.getString(LyricOrder.Local.stringRes)

  override suspend fun getLyrics(song: Song): LyricsResult {
    val path = getLocalLyricPath(song)
    if (path.isNotEmpty()) {
      return LyricsResult(LrcParser.parse(File(path).readBytes()), id)
    }

    throw Exception("no lyric found by local file")
  }

  private fun getLocalLyricPath(song: Song): String {
    var path = ""
    val searchKeys = getLocalSearchKey(song)
    val selection = StringBuilder()
    for (i in searchKeys.indices) {
      if (i != 0) {
        selection.append(" or ")
      }
      selection.append(MediaStore.Files.FileColumns.DATA).append(" like ?")
    }

    //没有设置歌词路径 搜索所有可能的歌词文件
    context.contentResolver.query(
      MediaStore.Files.getContentUri("external"), null,
      selection.toString(),
      searchKeys,
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
   * displayName-artist.lrc
   * artist-title.lrc
   * filename.lrc
   */
  private fun getLocalSearchKey(song: Song): Array<String> {
    val keys = ArrayList<String>()
    keys.add("%${song.artist}%${song.displayName}$SUFFIX_LYRIC")
    keys.add("%${song.displayName}$SUFFIX_LYRIC")
    keys.add("%${song.title}$SUFFIX_LYRIC")
    keys.add("%${song.title}%${song.artist}$SUFFIX_LYRIC")
    keys.add("%${song.displayName}%${song.artist}$SUFFIX_LYRIC")
    keys.add("%${song.artist}%${song.title}$SUFFIX_LYRIC")

    // 文件名匹配
    val filename = song.data.substringAfterLast(File.separatorChar).substringBeforeLast('.')
    if (filename.isNotEmpty()) {
      keys.add("%$filename$SUFFIX_LYRIC")
    }
    return keys.toTypedArray()
  }

  companion object {

    private const val SUFFIX_LYRIC = ".lrc"
  }
}
