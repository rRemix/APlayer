package remix.myplayer.util

import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Song

/**
 * 统一的搜索关键词生成工具类
 * 用于歌词搜索和封面搜索
 */
object SearchKeyUtil {

  enum class KeyKind { ARTIST_TITLE, TITLE, ALBUM_TITLE, FILE_NAME, ARTIST_ALBUM, ARTIST, ALBUM }
  data class SearchKey(val kind: KeyKind, val value: String)

  private val invalidValues = setOf(
    "unknown", "<unknown>", "未知歌曲", "未知艺术家", "未知专辑",
    "unknown artist", "unknown album", "unknown song",
    "null", "n/a", "na", "none", "无", "暂无",
    "track", "track 1", "track 01"
  )

  fun getSearchKeys(song: Song?): List<SearchKey> {
    if (song == null) return emptyList()
    return buildSongKeys(song)
  }

  fun getSearchKeys(model: Any?): List<SearchKey> {
    if (model == null) return emptyList()
    return when (model) {
      is Song -> buildSongKeys(model)
      is Album -> buildAlbumKeys(model)
      is Artist -> buildArtistKeys(model)
      else -> emptyList()
    }
  }

  /**
   * 信息验证
   */
  private fun isValidInfo(info: String?): Boolean {
    if (info.isNullOrBlank()) return false

    val trimmed = info.trim()
    val lowerCase = trimmed.lowercase()

    if (lowerCase in invalidValues) return false

    // 检查是否只包含数字（通常是无效的标题）
    if (trimmed.all { it.isDigit() }) return false

    // 检查是否包含过多特殊字符
    val specialCharCount = trimmed.count { !it.isLetterOrDigit() && !it.isWhitespace() }
    if (specialCharCount > trimmed.length * 0.5) return false

    return true
  }

  private fun String?.valid(): Boolean = isValidInfo(this)

  private fun buildSongKeys(song: Song): List<SearchKey> {
    val titleValid = song.title.valid()
    val artistValid = song.artist.valid()
    val albumValid = song.album.valid()

    val base = listOfNotNull(
      if (titleValid && artistValid) SearchKey(KeyKind.ARTIST_TITLE, "${song.artist} - ${song.title}") else null,
      if (titleValid) SearchKey(KeyKind.TITLE, song.title) else null,
      if (titleValid && albumValid && !artistValid) SearchKey(KeyKind.ALBUM_TITLE, "${song.album} - ${song.title}") else null
    ).distinctBy { it.value }

    if (base.isEmpty() && song.isLocal()) {
      val fileName = song.displayName
      if (fileName.isNotBlank()) return listOf(SearchKey(KeyKind.FILE_NAME, fileName))
    }
    return base
  }

  private fun buildAlbumKeys(model: Album): List<SearchKey> {
    val albumValid = model.album.valid()
    val artistValid = model.artist.valid()

    return listOfNotNull(
      if (albumValid && artistValid) SearchKey(KeyKind.ARTIST_ALBUM, "${model.artist} - ${model.album}") else null,
      if (albumValid) SearchKey(KeyKind.ALBUM, model.album) else null
    ).distinctBy { it.value }
  }

  private fun buildArtistKeys(model: Artist): List<SearchKey> {
    val artistValid = model.artist.valid()
    return listOfNotNull(
      if (artistValid) SearchKey(KeyKind.ARTIST, model.artist) else null
    )
  }
}