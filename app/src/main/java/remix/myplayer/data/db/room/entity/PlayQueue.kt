package remix.myplayer.data.db.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId

@Entity(primaryKeys = ["source_id", "source_key"], indices = [Index(value = ["position"], unique = true)])
data class PlayQueue(
  @ColumnInfo(name = "source_id") val sourceId: String,
  @ColumnInfo(name = "source_key") val sourceKey: String,
  val position: Int
) {
  fun identity() = SongIdentity(SourceId(sourceId), sourceKey)
  companion object { const val TABLE_NAME = "PlayQueue" }
}
