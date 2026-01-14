package remix.myplayer.data.model.lastfm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    @SerialName("#text")
    var text: String? = null,
    var size: String? = null
)
