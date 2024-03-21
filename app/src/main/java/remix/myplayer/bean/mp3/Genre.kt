package remix.myplayer.bean.mp3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Genre(val id: Long, val genre: String, val count: Int) : Parcelable, APlayerModel {

  override fun getKey(): String {
    return id.toString()
  }
}