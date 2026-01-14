package remix.myplayer.data.model.lastfm

import kotlinx.serialization.Serializable

@Serializable
class LastFmAlbum {
  var album: Album? = null

  @Serializable
  class Album {
    var image: List<Image> = ArrayList()

  }
}
