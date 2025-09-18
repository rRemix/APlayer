package remix.myplayer.compose.lyric.provider

import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.util.ImageUriUtil

interface ILyricsProvider {
  val id: String
  val displayName: String

  /**
   * 返回的 List 为空不视为失败，仅抛出异常视为失败
   *
   * @throws Throwable
   */
  suspend fun getLyrics(song: Song): List<LyricsLine>

  companion object {

    // TODO
    fun getLyricSearchKey(song: Song?): String {
      if (song == null)
        return ""
      val isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(song.title)
      val isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(song.album)
      val isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(song.artist)

      //歌曲名合法
      return if (isTitleAvailable) {
        when {
          isArtistAvailable -> song.artist + "-" + song.title //艺术家合法
          isAlbumAvailable -> //专辑名合法
            song.album + "-" + song.title
          else -> song.title
        }
      } else ""
    }
  }
}
