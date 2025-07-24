package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.net.Uri
import java.io.Serial
import androidx.core.net.toUri
import kotlinx.serialization.Serializable

/**
 * Created by Remix on 2017/10/22.
 */

@Serializable
data class Album(val albumID: Long,
                 val album: String,
                 val artistID: Long,
                 val artist: String,
                 var count: Int = 0) : APlayerModel {
  val artUri: Uri
    get() = ContentUris.withAppendedId("content://media/external/audio/albumart/".toUri(), albumID)

  override fun getKey(): String {
    return albumID.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 1214925651243602701L
  }
}
