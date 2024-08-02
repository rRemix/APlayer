package remix.myplayer.bean.mp3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serial

/**
 * Created by Remix on 2017/10/22.
 */

@Parcelize
data class Artist(val artistID: Long,
                  val artist: String,
                  var count: Int) : Parcelable, APlayerModel {

  override fun getKey(): String {
    return artistID.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = -8314996381751820478L
  }
}
