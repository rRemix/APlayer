package remix.myplayer.db.room.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity(indices = [Index(value = ["audio_id"], unique = true)])
data class PlayQueue(
    val audio_id: Long,
    val title: String,
    val data: String
) {
  constructor(audio_id: Long): this(audio_id, "", "")

  @PrimaryKey(autoGenerate = true)
  var id: Int = 0

  var account: String? = null
  var pwd: String? = null


  companion object {
    const val TABLE_NAME = "PlayQueue"
  }
}