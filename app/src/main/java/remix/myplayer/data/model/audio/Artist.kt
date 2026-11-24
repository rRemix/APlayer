package remix.myplayer.data.model.audio

import kotlinx.serialization.Serializable
import java.io.Serial

/**
 * Created by Remix on 2017/10/22.
 */
@Serializable
data class Artist(val artistId: Long,
                  val artist: String,
                  val count: Int) : APlayerModel {

  override fun getKey(): String {
    return artistId.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = -8314996381751820478L
  }
}
