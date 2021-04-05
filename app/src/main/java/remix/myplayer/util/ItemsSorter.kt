package remix.myplayer.util

import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.SortOrder

object ItemsSorter {
  private fun compare(o1: String, o2: String): Int {
    var i = 0
    while (i < o1.length && i < o2.length) {
      if (Pinyin.isChinese(o1[i]) != Pinyin.isChinese(o2[i]))
        return if (Pinyin.isChinese(o1[i])) -1 else 1
      var t = Pinyin.toPinyin(o1[i]).compareTo(Pinyin.toPinyin(o2[i]))
      if (t != 0)
        return t
      t = o1[i].compareTo(o2[i])
      if (t != 0)
        return t
      ++i
    }
    return o1.length.compareTo(o2.length)
  }

  fun sortedSongs(songs: List<Song>, sortOrder: String?): List<Song> {
    return songs.sortedWith(Comparator { o1: Song, o2: Song ->
      when (sortOrder) {
        SortOrder.SONG_A_Z -> compare(o1.title, o2.title)
        SortOrder.SONG_Z_A -> compare(o2.title, o1.title)
        SortOrder.ARTIST_A_Z -> compare(o1.artist, o2.artist)
        SortOrder.ARTIST_Z_A -> compare(o2.artist, o1.artist)
        SortOrder.ALBUM_A_Z -> compare(o1.album, o2.album)
        SortOrder.ALBUM_Z_A -> compare(o2.album, o1.album)
        SortOrder.DATE -> o1.addTime.compareTo(o2.addTime)
        SortOrder.DATE_DESC -> o2.addTime.compareTo(o1.addTime)
        SortOrder.DISPLAY_NAME_A_Z -> compare(o1.displayName, o2.displayName)
        SortOrder.DISPLAY_NAME_Z_A -> compare(o2.displayName, o1.displayName)
        else -> 0
      }
    })
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
    return playLists.sortedWith(Comparator { o1: PlayList, o2: PlayList ->
      when (sortOrder) {
        SortOrder.PLAYLIST_A_Z -> compare(o1.name, o2.name)
        SortOrder.PLAYLIST_Z_A -> compare(o2.name, o1.name)
        SortOrder.PLAYLIST_DATE -> o1.date.compareTo(o2.date)
        else -> 0
      }
    })
  }
}