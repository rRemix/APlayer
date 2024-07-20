package remix.myplayer.db.room.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class WebDav(
  var alias: String,
  var account: String,
  var pwd: String,
  var server: String,
  var lastUrl: String,
  val createAt: Long = System.currentTimeMillis()
) : Serializable {
  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

  fun base(): String {
    val uri = Uri.parse(server)
    var url = "${uri.scheme}://${uri.host}"
    if (uri.port > 0) {
      url = "${url}:${uri.port}"
    }
    return url
  }
  
}