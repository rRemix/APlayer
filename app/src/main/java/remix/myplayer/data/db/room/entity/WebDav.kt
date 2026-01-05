package remix.myplayer.data.db.room.entity

import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@kotlinx.serialization.Serializable
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
    val uri = server.toUri()
    var url = "${uri.scheme}://${uri.host}"
    if (uri.port > 0) {
      url = "${url}:${uri.port}"
    }
    return url
  }

  fun getRoot(): String {
    return server.removeSuffix("/")
  }

  fun generateUrl(path: String): String {
    return base() + path
  }

  fun buildPathStack(currentUrl: String): List<String> {
    val root = getRoot()
    val current = currentUrl.removeSuffix("/")
    return if (current.startsWith(root)) {
      current.removePrefix(root)
        .trimStart('/')
        .split('/')
        .filter { it.isNotEmpty() }
        .runningFold(root) { acc, part -> "$acc/$part" }
    } else {
      listOf(currentUrl)
    }
  }
}