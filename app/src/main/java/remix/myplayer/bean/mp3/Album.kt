package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serial

/**
 * Created by Remix on 2017/10/22.
 */

@Parcelize
data class Album(val albumID: Long,
                 val album: String,
                 val artistID: Long,
                 val artist: String,
                 var count: Int = 0) : Parcelable, APlayerModel {
  val artUri: Uri
    get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumID)

  override fun getKey(): String {
    return albumID.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 1214925651243602701L
  }
}
