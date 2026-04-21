package remix.myplayer.data.model.misc

import remix.myplayer.R
import remix.myplayer.helper.SortOrder
import java.io.Serializable

@kotlinx.serialization.Serializable
data class Library(
  val tag: Int,
  val enable: Boolean = true
) : Serializable {

  val stringRes: Int
    get() = when (tag) {
      TAG_SONG -> R.string.tab_song
      TAG_ALBUM -> R.string.tab_album
      TAG_ARTIST -> R.string.tab_artist
      TAG_PLAYLIST -> R.string.tab_playlist
      TAG_GENRE -> R.string.tab_genre
      TAG_FOLDER -> R.string.tab_folder
      TAG_REMOTE -> R.string.tab_remote
      else -> throw IllegalArgumentException("unknown tag: $tag")
    }

  val menuItems: List<Int>
    get() = when (tag) {
      TAG_SONG -> listOf(
        R.string.title,
        R.string.title_desc,
        R.string.display_title,
        R.string.display_title_desc,
        R.string.album,
        R.string.album_desc,
        R.string.artist,
        R.string.artist_desc,
        R.string.date_modify,
        R.string.date_modify_desc
      )

      TAG_ALBUM -> listOf(
        R.string.album,
        R.string.album_desc,
        R.string.artist,
        R.string.artist_desc
      )

      TAG_ARTIST -> listOf(
        R.string.artist,
        R.string.artist_desc
      )

      TAG_PLAYLIST -> listOf(
        R.string.name,
        R.string.name_desc,
        R.string.create_time
      )

      TAG_GENRE -> listOf(
        R.string.genre,
        R.string.genre_desc
      )

      TAG_FOLDER -> listOf(
        R.string.name,
        R.string.name_desc
      )

      else -> throw IllegalArgumentException("unknown tag: $tag")
    }

  val sortOrders: List<String>
    get() = when (tag) {
      TAG_SONG -> listOf(
        SortOrder.SONG_A_Z,
        SortOrder.SONG_Z_A,
        SortOrder.DISPLAY_NAME_A_Z,
        SortOrder.DISPLAY_NAME_Z_A,
        SortOrder.ALBUM_A_Z,
        SortOrder.ALBUM_Z_A,
        SortOrder.ARTIST_A_Z,
        SortOrder.ARTIST_Z_A,
        SortOrder.DATE,
        SortOrder.DATE_DESC
      )

      TAG_ALBUM -> listOf(
        SortOrder.ALBUM_A_Z,
        SortOrder.ALBUM_Z_A,
        SortOrder.ARTIST_A_Z,
        SortOrder.ARTIST_Z_A,
      )

      TAG_ARTIST -> listOf(
        SortOrder.ARTIST_A_Z,
        SortOrder.ARTIST_Z_A,
      )

      TAG_PLAYLIST -> listOf(
        SortOrder.PLAYLIST_A_Z,
        SortOrder.PLAYLIST_Z_A,
        SortOrder.PLAYLIST_DATE
      )

      TAG_GENRE -> listOf(
        SortOrder.GENRE_A_Z,
        SortOrder.GENRE_Z_A
      )

      TAG_FOLDER -> listOf(
        SortOrder.FOLDER_A_Z,
        SortOrder.FOLDER_Z_A
      )

      else -> throw IllegalArgumentException("unknown tag: $tag")
    }

  companion object {

    const val TAG_SONG = 0
    const val TAG_ALBUM = 1
    const val TAG_ARTIST = 2
    const val TAG_GENRE = 3
    const val TAG_PLAYLIST = 4
    const val TAG_FOLDER = 5
    const val TAG_REMOTE = 6

    val default = listOf(
      Library(TAG_SONG),
      Library(TAG_ALBUM),
      Library(TAG_ARTIST),
      Library(TAG_GENRE),
      Library(TAG_PLAYLIST),
      Library(TAG_FOLDER),
      Library(TAG_REMOTE)
    )

  }

}
