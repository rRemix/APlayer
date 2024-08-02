package remix.myplayer.bean.mp3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serial

@Parcelize
class Genre(val id: Long, val genre: String, val count: Int) : Parcelable, APlayerModel {

  override fun getKey(): String {
    return id.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = -2424542670832627129L
  }
}