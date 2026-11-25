package remix.myplayer.ui.widget.library

import org.junit.Assert.assertEquals
import org.junit.Test
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId

class AlbumCoverCollageTest {
  @Test
  fun remoteSongsWithDefaultAlbumIdsCanSupplyFourCovers() {
    val songs = (1..6).map { remote(it.toString()) }

    assertEquals(songs.take(4), selectCoverSongs(songs) { false })
    // Failed covers do not consume a slot; later remote songs can replace them.
    assertEquals(songs.drop(2), selectCoverSongs(songs) { it in songs.take(2) })
  }

  @Test
  fun localAlbumsKeepOneCoverAndTryAnotherSongAfterFailure() {
    val first = local(1, albumId = 10)
    val fallback = local(2, albumId = 10)
    val otherAlbum = local(3, albumId = 20)
    val songs = listOf(first, fallback, otherAlbum)

    assertEquals(listOf(first, otherAlbum), selectCoverSongs(songs) { false })
    assertEquals(listOf(fallback, otherAlbum), selectCoverSongs(songs) { it == first })
  }

  @Test
  fun missingLocalAlbumIdsAndDifferentRemoteSourcesDoNotCollapse() {
    val songs = listOf(
      local(1, albumId = 0),
      local(2, albumId = 0),
      remote("same.flac", sourceId = SourceId("webdav:1")),
      remote("same.flac", sourceId = SourceId("webdav:2"))
    )

    assertEquals(songs, selectCoverSongs(songs) { false })
  }

  private fun local(id: Int, albumId: Long) = Song(
    SourceId.MEDIASTORE, id.toString(), "/music/$id.flac", "$id.flac", albumId = albumId
  )

  private fun remote(key: String, sourceId: SourceId = SourceId("webdav:1")) = Song(
    sourceId, key, "https://dav.test/$key", key
  )
}
