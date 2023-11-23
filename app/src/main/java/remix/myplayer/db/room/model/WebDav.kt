package remix.myplayer.db.room.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class WebDav(
  var alias: String,
  var account: String?,
  var pwd: String?,
  var server: String,
  var lastPath: String? = null,
  val createAt: Long = System.currentTimeMillis()
) : Serializable {
  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

  fun isRoot(url: String): Boolean {
    return url.removePrefix("/") == root()
  }

  fun root() = server.removeSuffix("/")

  fun initialPath(): String {
    val uri = Uri.parse(server)
    return uri.path ?: ""
  }

  fun base(): String {
    val uri = Uri.parse(server)
    return "${uri.scheme}://${uri.host}"
  }

  fun last(): String {
    return if (!lastPath.isNullOrEmpty()) {
      var lastPath = lastPath!!
      if (!lastPath.startsWith("/")) {
        lastPath = "/$lastPath"
      }
      base().plus(lastPath).removeSuffix("/")
    } else {
      root()
    }
  }
}