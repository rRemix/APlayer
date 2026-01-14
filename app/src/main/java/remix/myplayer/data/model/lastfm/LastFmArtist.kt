package remix.myplayer.data.model.lastfm

import kotlinx.serialization.Serializable

@Serializable
class LastFmArtist {

  var artist: Artist? = null

  @Serializable
  class Artist {

    var image: List<Image> = ArrayList()
  }
}
