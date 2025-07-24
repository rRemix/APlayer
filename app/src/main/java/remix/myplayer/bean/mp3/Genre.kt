package remix.myplayer.bean.mp3

import kotlinx.serialization.Serializable
import java.io.Serial

@Serializable
class Genre(val id: Long, val genre: String, val count: Int) : APlayerModel {

  override fun getKey(): String {
    return id.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = -2424542670832627129L
  }
}