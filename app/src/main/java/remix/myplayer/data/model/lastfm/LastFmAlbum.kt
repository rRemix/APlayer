package remix.myplayer.data.model.lastfm

import com.google.gson.annotations.Expose
import kotlinx.serialization.Serializable

@Serializable
class LastFmAlbum {
  @Expose
  var album: Album? = null

  @Serializable
  class Album {
    @Expose
    var image: List<Image> = ArrayList()

  }
}
