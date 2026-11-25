package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["name"], unique = true)])
data class PlayList(
  @PrimaryKey(autoGenerate = true) val id: Long,
  val name: String,
  val date: Long
) {
  companion object { const val TABLE_NAME = "PlayList" }
}
