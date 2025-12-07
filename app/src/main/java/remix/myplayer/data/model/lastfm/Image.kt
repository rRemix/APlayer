package remix.myplayer.data.model.lastfm

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    @SerializedName("#text")
    @Expose
    var text: String? = null,
    @Expose
    var size: String? = null
)