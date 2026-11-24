package remix.myplayer.data.model.audio

import android.content.ContentUris
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import java.io.Serial

/**
 * Created by Remix on 2017/10/22.
 */

@Serializable
data class Album(val albumId: Long,
                 val album: String,
                 val artistId: Long,
                 val artist: String,
                 val count: Int = 0) : APlayerModel {
  val artUri: Uri
    get() = ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)

  override fun getKey(): String {
    return albumId.toString()
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 1214925651243602701L
  }
}
