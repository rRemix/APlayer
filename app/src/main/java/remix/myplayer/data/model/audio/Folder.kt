package remix.myplayer.data.model.audio

import kotlinx.serialization.Serializable
import java.io.Serial

/**
 * Created by Remix on 2018/1/9.
 */
@Serializable
data class Folder(
  val name: String?,
  val count: Int,
  val path: String
) : APlayerModel {

  override fun getKey(): String {
    return path
  }

  companion object {

    @Serial
    private const val serialVersionUID: Long = -7333769143033322264L
  }
}
