package remix.myplayer.bean.misc

import remix.myplayer.bean.mp3.APlayerModel
import java.io.Serializable

data class CustomCover(
    val model: APlayerModel,
    val type: Int) : Serializable