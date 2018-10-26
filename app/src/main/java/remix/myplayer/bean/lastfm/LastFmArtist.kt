package remix.myplayer.bean.lastfm

import com.google.gson.annotations.Expose
import java.util.*

class LastFmArtist {
    @Expose
    var artist: Artist? = null

    class Artist {
        @Expose
        var image: List<Image> = ArrayList()


    }
}
