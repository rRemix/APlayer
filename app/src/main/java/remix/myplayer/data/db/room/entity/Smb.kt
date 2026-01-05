package remix.myplayer.data.db.room.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@kotlinx.serialization.Serializable
@Entity
data class Smb(
  var alias: String,
  var domain: String?,
  var account: String,
  var pwd: String,
  var server: String,
  var share: String,
  var lastUrl: String,
  val createAt: Long = System.currentTimeMillis()
) : Serializable {

  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

  fun getRoot(): String {
    val normalizedServer = server.removeSuffix("/")
    val normalizedShare = share.trim { it == '/' || it == '\\' }
    return if (normalizedShare.isEmpty()) {
      normalizedServer
    } else {
      "$normalizedServer/$normalizedShare"
    }
  }

  fun getHost(): String {
    return server.removePrefix("smb://")
  }

  fun getRelativePath(path: String): String {
    val root = getRoot()
    return if (path.startsWith(root)) {
      path.removePrefix(root).trimStart('/')
    } else {
      path.trimStart('/')
    }
  }

  fun buildPathStack(currentUrl: String): List<String> {
    val root = getRoot()
    val current = currentUrl.removeSuffix("/")
    val relative = if (current.startsWith(root)) {
      current.removePrefix(root).trimStart('/')
    } else {
      current.trimStart('/')
    }

    return relative
      .split('/')
      .filter { it.isNotEmpty() }
      .runningFold(root) { acc, part -> "$acc/$part" }
  }

  fun generateUri(path: String): String {
    val relativePath = getRelativePath(path)

    var userInfo = ""
    if (!domain.isNullOrEmpty()) {
      userInfo += "${Uri.encode(domain)};"
    }
    userInfo += Uri.encode(account)
    if (pwd.isNotEmpty()) {
      userInfo += ":${Uri.encode(pwd)}"
    }

    val serverHost = getHost()
    val encodedShare = Uri.encode(share)

    val segments = relativePath.replace('\\', '/').split("/").filter { it.isNotEmpty() }
    val encodedPath = segments.joinToString("/") { Uri.encode(it) }

    return "smb://$userInfo@$serverHost/$encodedShare/$encodedPath"
  }

  companion object {
    fun parseServerAddress(server: String): Pair<String, Int?> {
      var host = server.removePrefix("smb://")
      var port: Int? = null
      if (host.contains(":")) {
        val parts = host.split(":")
        host = parts[0]
        port = parts.getOrNull(1)?.toIntOrNull()
      }
      return host to port
    }
  }
}
