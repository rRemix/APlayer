package remix.myplayer.helper

import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import kotlin.math.min

object ItemsSorter {

  // 逐字符比较（与原版 APlayer 一致）：
  // - 中文排在非中文前面；都是中文则按拼音比较；
  // - 非中文先忽略大小写比较，再用区分大小写兜底；
  // - 数字就是普通 ASCII 字符（code-point 顺序），因此 1/2/10 会按 1,10,2 排列。
  private fun compare(o1: String, o2: String): Int {
    val minLength = min(o1.length, o2.length)
    for (i in 0 until minLength) {
      val c1 = o1[i]
      val c2 = o2[i]
      if (Pinyin.isChinese(c1) != Pinyin.isChinese(c2)) {
        return if (Pinyin.isChinese(c1)) -1 else 1
      }
      if (Pinyin.isChinese(c1) && Pinyin.isChinese(c2)) {
        val pinyinResult = Pinyin.toPinyin(c1).compareTo(Pinyin.toPinyin(c2))
        if (pinyinResult != 0) return pinyinResult
      } else {
        val lowerResult = c1.lowercaseChar().compareTo(c2.lowercaseChar())
        if (lowerResult != 0) return lowerResult
      }
      val rawResult = c1.compareTo(c2)
      if (rawResult != 0) return rawResult
    }
    return o1.length.compareTo(o2.length)
  }

  fun sortedSongs(songs: List<Song>, sortOrder: String?): List<Song> {
    return when (sortOrder) {
      SortOrder.DATE, SortOrder.DATE_DESC, SortOrder.TRACK_NUMBER -> songs
      else -> {
        songs.sortedWith(Comparator { o1: Song, o2: Song ->
          when (sortOrder) {
            SortOrder.SONG_A_Z -> compare(o1.title, o2.title)
            SortOrder.SONG_Z_A -> compare(o2.title, o1.title)
            SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist)
            SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist)
            SortOrder.ALBUM_A_Z -> compare(o1.album, o2.album)
            SortOrder.ALBUM_Z_A -> compare(o2.album, o1.album)
            SortOrder.DISPLAY_NAME_A_Z -> compare(o1.displayName, o2.displayName)
            SortOrder.DISPLAY_NAME_Z_A -> compare(o2.displayName, o1.displayName)
            else -> 0
          }
        })
      }
    }
  }

  fun sortedAlbums(albums: List<Album>, sortOrder: String?): List<Album> {
    return albums.sortedWith(Comparator { o1: Album, o2: Album ->
      when (sortOrder) {
        SortOrder.ALBUM_A_Z -> compare(o1.album, o2.album)
        SortOrder.ALBUM_Z_A -> compare(o2.album, o1.album)
        SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist)
        SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist)
        else -> 0
      }
    })
  }

  fun sortedArtists(artists: List<Artist>, sortOrder: String?): List<Artist> {
    return artists.sortedWith(Comparator { o1: Artist, o2: Artist ->
      when (sortOrder) {
        SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist)
        SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist)
        else -> 0
      }
    })
  }

  fun sortedPlayLists(playLists: List<PlayList>, sortOrder: String?): List<PlayList> {
    return when (sortOrder) {
      SortOrder.PLAYLIST_DATE -> playLists
      else -> {
        playLists.sortedWith(Comparator { o1: PlayList, o2: PlayList ->
          when (sortOrder) {
            SortOrder.PLAYLIST_A_Z -> compare(o1.name, o2.name)
            SortOrder.PLAYLIST_Z_A -> compare(o2.name, o1.name)
            else -> 0
          }
        })
      }
    }
  }

  fun sortedGenres(genres: List<Genre>, sortOrder: String?): List<Genre> {
    return genres.sortedWith(Comparator { o1: Genre, o2: Genre ->
      when (sortOrder) {
        SortOrder.GENRE_A_Z -> compare(o1.genre, o2.genre)
        SortOrder.GENRE_Z_A -> compare(o2.genre, o1.genre)
        else -> 0
      }
    })
  }

  fun sortedFolders(folders: List<Folder>, sortOrder: String?): List<Folder> {
    return folders.sortedWith(Comparator { o1: Folder, o2: Folder ->
      val compareResult = when (sortOrder) {
        SortOrder.FOLDER_A_Z -> compare(o1.name ?: o1.path, o2.name ?: o2.path)
        SortOrder.FOLDER_Z_A -> compare(o2.name ?: o2.path, o1.name ?: o1.path)
        else -> 0
      }
      if (compareResult != 0) compareResult else compare(o1.path, o2.path)
    })
  }
}
