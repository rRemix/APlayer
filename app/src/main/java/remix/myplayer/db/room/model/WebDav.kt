package remix.myplayer.db.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class WebDav(
  var alias: String,
  var account: String?,
  var pwd: String?,
  var server: String,
  var initialPath: String,
  var lastPath: String? = null,
  val createAt: Long = System.currentTimeMillis()
) : Serializable {
  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

  fun root(): String {
    return if (initialPath.isNotEmpty()) {
      server.plus(initialPath)
    } else {
      server
    }.removeSuffix("/")
  }

  fun last(): String {
    return if (!lastPath.isNullOrEmpty()) {
      root().plus("$lastPath")
    } else {
      root()
    }
  }
}