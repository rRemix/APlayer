package remix.myplayer.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId

class DetailStateTest {
  private val album = Album(10, "复现测试专辑", 1, "路由中的旧歌手")

  @Test
  fun albumHeaderUsesAllLoadedTrackArtistsInAStableOrder() {
    val songs = listOf(
      song(1, " 测试歌手B "),
      song(2, "测试歌手A"),
      song(3, "测试歌手B"),
      song(4, " "),
    )

    assertEquals("测试歌手A, 测试歌手B", detailState(album, songs).albumArtistText)
    assertEquals("测试歌手A, 测试歌手B", detailState(album, songs.reversed()).albumArtistText)
  }

  @Test
  fun compilationShowsAlbumArtistInsteadOfEveryTrackArtist() {
    val songs = listOf(
      song(1, "测试歌手A", albumArtist = " 群星 "),
      song(2, "测试歌手B", albumArtist = "群星"),
    )

    assertEquals("群星", detailState(album, songs).albumArtistText)
  }

  @Test
  fun availableAlbumArtistTakesPriorityWhenSomeTracksHaveNoAlbumArtist() {
    val songs = listOf(
      song(1, "测试歌手A", albumArtist = " "),
      song(2, "测试歌手B", albumArtist = "群星"),
    )

    assertEquals("群星", detailState(album, songs).albumArtistText)
  }

  @Test
  fun multipleAlbumArtistCreditsAreDeduplicatedWithoutChangingTheAlbumContents() {
    val songs = listOf(
      song(1, "演唱者", albumArtist = "专辑艺术家B"),
      song(2, "演唱者", albumArtist = "专辑艺术家A"),
      song(3, "演唱者", albumArtist = "专辑艺术家B"),
    )
    val state = detailState(album, songs)

    assertEquals("专辑艺术家A, 专辑艺术家B", state.albumArtistText)
    assertEquals(songs, state.songs)
  }

  @Test
  fun collaborationAndPunctuationInArtistCreditsArePreserved() {
    val songs = listOf(
      song(1, "测试歌手A & 测试歌手B"),
      song(2, "Earth, Wind & Fire"),
      song(3, "测试歌手A & 测试歌手B"),
    )

    assertEquals("Earth, Wind & Fire, 测试歌手A & 测试歌手B", albumArtistText(songs))
  }

  @Test
  fun emptyCreditsAndNonAlbumDetailsDoNotShowAlbumArtistText() {
    assertEquals("", detailState(album, emptyList()).albumArtistText)
    assertEquals("", detailState(album, listOf(song(1, " "))).albumArtistText)
    assertEquals("", detailState(Artist(1, "测试歌手A", 0), emptyList()).albumArtistText)
  }

  private fun song(id: Int, artist: String, albumArtist: String = "") = Song(
    SourceId.MEDIASTORE, id.toString(), "/music/$id.mp3", "$id.mp3",
    albumId = album.albumId, artist = artist, albumArtist = albumArtist,
  )
}
