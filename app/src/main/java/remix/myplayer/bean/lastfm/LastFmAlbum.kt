package remix.myplayer.bean.lastfm

import com.google.gson.annotations.Expose
import kotlinx.serialization.Serializable
import java.util.*

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
