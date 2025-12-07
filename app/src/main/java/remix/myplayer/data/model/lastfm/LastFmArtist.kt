package remix.myplayer.data.model.lastfm

import com.google.gson.annotations.Expose
import kotlinx.serialization.Serializable

@Serializable
class LastFmArtist {

  @Expose
  var artist: Artist? = null

  @Serializable
  class Artist {

    @Expose
    var image: List<Image> = ArrayList()
  }
}
