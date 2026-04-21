package remix.myplayer.helper

import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import java.text.Collator

object ItemsSorter {

  // 自然排序：文本段用 Collator 数字段按数值语义比较
  private fun compare(o1: String, o2: String, collator: Collator): Int {
    var i = 0
    var j = 0

    while (i < o1.length && j < o2.length) {
      val c1 = o1[i]
      val c2 = o2[j]

      if (c1.isAsciiDigit() && c2.isAsciiDigit()) {
        // 两侧都遇到数字：提取连续数字段并按数值比较
        val start1 = i
        val start2 = j
        while (i < o1.length && o1[i].isAsciiDigit()) {
          i++
        }
        while (j < o2.length && o2[j].isAsciiDigit()) {
          j++
        }

        val numberResult = compareNumberSegment(
          o1.substring(start1, i),
          o2.substring(start2, j)
        )
        if (numberResult != 0) {
          return numberResult
        }
      } else {
        // 文本段比较使用 Collator
        val start1 = i
        val start2 = j
        while (i < o1.length && !o1[i].isAsciiDigit()) {
          i++
        }
        while (j < o2.length && !o2[j].isAsciiDigit()) {
          j++
        }

        val textResult = collator.compare(
          o1.substring(start1, i),
          o2.substring(start2, j)
        )
        if (textResult != 0) {
          return textResult
        }
      }
    }

    return o1.length.compareTo(o2.length)
  }

  private fun compareNumberSegment(n1: String, n2: String): Int {
    // 去前导零后先比有效数字长度，再按字典序比较，避免大数转整数溢出
    val v1 = n1.trimStart('0').ifEmpty { "0" }
    val v2 = n2.trimStart('0').ifEmpty { "0" }

    if (v1.length != v2.length) {
      return v1.length.compareTo(v2.length)
    }

    val valueResult = v1.compareTo(v2)
    if (valueResult != 0) {
      return valueResult
    }

    // 数值相同（如 1 和 01）时，短的在前，保证排序稳定且更符合直觉
    return n1.length.compareTo(n2.length)
  }

  fun sortedSongs(songs: List<Song>, sortOrder: String?): List<Song> {
    val collator = Collator.getInstance()
    return when (sortOrder) {
      SortOrder.DATE, SortOrder.DATE_DESC, SortOrder.TRACK_NUMBER -> songs
      else -> {
        songs.sortedWith(Comparator { o1: Song, o2: Song ->
          when (sortOrder) {
            SortOrder.SONG_A_Z -> compare(o1.title, o2.title, collator)
            SortOrder.SONG_Z_A -> compare(o2.title, o1.title, collator)
            SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist, collator)
            SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist, collator)
            SortOrder.ALBUM_A_Z -> compare(o1.album, o2.album, collator)
            SortOrder.ALBUM_Z_A -> compare(o2.album, o1.album, collator)
            SortOrder.DISPLAY_NAME_A_Z -> compare(o1.displayName, o2.displayName, collator)
            SortOrder.DISPLAY_NAME_Z_A -> compare(o2.displayName, o1.displayName, collator)
            else -> 0
          }
        })
      }
    }
  }

  fun sortedAlbums(albums: List<Album>, sortOrder: String?): List<Album> {
    val collator = Collator.getInstance()
    return albums.sortedWith(Comparator { o1: Album, o2: Album ->
      when (sortOrder) {
        SortOrder.ALBUM_A_Z -> compare(o1.album, o2.album, collator)
        SortOrder.ALBUM_Z_A -> compare(o2.album, o1.album, collator)
        SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist, collator)
        SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist, collator)
        else -> 0
      }
    })
  }

  fun sortedArtists(artists: List<Artist>, sortOrder: String?): List<Artist> {
    val collator = Collator.getInstance()
    return artists.sortedWith(Comparator { o1: Artist, o2: Artist ->
      when (sortOrder) {
        SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist, collator)
        SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist, collator)
        else -> 0
      }
    })
  }

  fun sortedPlayLists(playLists: List<PlayList>, sortOrder: String?): List<PlayList> {
    val collator = Collator.getInstance()
    return when (sortOrder) {
      SortOrder.PLAYLIST_DATE -> playLists
      else -> {
        playLists.sortedWith(Comparator { o1: PlayList, o2: PlayList ->
          when (sortOrder) {
            SortOrder.PLAYLIST_A_Z -> compare(o1.name, o2.name, collator)
            SortOrder.PLAYLIST_Z_A -> compare(o2.name, o1.name, collator)
            else -> 0
          }
        })
      }
    }
  }

  fun sortedGenres(genres: List<Genre>, sortOrder: String?): List<Genre> {
    val collator = Collator.getInstance()
    return genres.sortedWith(Comparator { o1: Genre, o2: Genre ->
      when (sortOrder) {
        SortOrder.GENRE_A_Z -> compare(o1.genre, o2.genre, collator)
        SortOrder.GENRE_Z_A -> compare(o2.genre, o1.genre, collator)
        else -> 0
      }
    })
  }

  fun sortedFolders(folders: List<Folder>, sortOrder: String?): List<Folder> {
    val collator = Collator.getInstance()
    return folders.sortedWith(Comparator { o1: Folder, o2: Folder ->
      val compareResult = when (sortOrder) {
        SortOrder.FOLDER_A_Z -> compare(o1.name ?: o1.path, o2.name ?: o2.path, collator)
        SortOrder.FOLDER_Z_A -> compare(o2.name ?: o2.path, o1.name ?: o1.path, collator)
        else -> 0
      }
      if (compareResult != 0) compareResult else compare(o1.path, o2.path, collator)
    })
  }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
