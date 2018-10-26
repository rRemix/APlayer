package remix.myplayer.bean.lastfm

import com.google.gson.annotations.Expose
import java.util.*

class LastFmAlbum {
    @Expose
    var album: Album? = null

    class Album {
        @Expose
        var image: List<Image> = ArrayList()

    }
}
