package remix.myplayer.bean.lastfm

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Image(
        @SerializedName("#text")
        @Expose
        var text: String? = null,
        @Expose
        var size: String? = null
)