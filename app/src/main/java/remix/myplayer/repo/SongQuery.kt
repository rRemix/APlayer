package remix.myplayer.repo

import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.ItemsSorter

enum class PathMatch { EXACT, SUFFIX }

/** Criteria over the loaded library. Path values are literal, never SQL patterns. */
data class SongQuery(
  val text: String? = null,
  val albumId: Long? = null,
  val artistId: Long? = null,
  val addedAfter: Long? = null,
  val path: String? = null,
  val pathMatch: PathMatch = PathMatch.EXACT,
  val folderPath: String? = null,
  val displayName: String? = null,
  val sortOrder: String? = null
) {
  internal fun applyTo(songs: List<Song>): List<Song> {
    val normalizedPath = path?.replace('\\', '/')
    return ItemsSorter.sortedSongs(songs.filter { song ->
      (albumId == null || song.albumId == albumId) &&
        (artistId == null || song.artistId == artistId) &&
        (addedAfter == null || song.addedAt >= addedAfter) &&
        (folderPath == null || song.data.substringBeforeLast('/', "") == folderPath) &&
        (displayName == null || song.displayName.equals(displayName, ignoreCase = true)) &&
        (normalizedPath == null || normalizedPath.isNotBlank() && when (pathMatch) {
          PathMatch.EXACT -> song.data == normalizedPath
          PathMatch.SUFFIX -> song.data.endsWith("/" + normalizedPath.trimStart('/'), ignoreCase = true)
        }) &&
        (text.isNullOrEmpty() || listOf(song.title, song.artist, song.album, song.displayName)
          .any { it.contains(text, ignoreCase = true) })
    }, sortOrder)
  }
}
