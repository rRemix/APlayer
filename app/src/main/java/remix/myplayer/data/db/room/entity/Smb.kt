package remix.myplayer.data.db.room.entity

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
  var lastPath: String,
  val createAt: Long = System.currentTimeMillis()
) : Serializable {

  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

}
