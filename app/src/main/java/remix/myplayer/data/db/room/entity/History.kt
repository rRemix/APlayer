package remix.myplayer.data.db.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId

@Entity(primaryKeys = ["source_id", "source_key"])
data class History(
  @ColumnInfo(name = "source_id") val sourceId: String,
  @ColumnInfo(name = "source_key") val sourceKey: String,
  val play_count: Int,
  val last_play: Long
) {
  fun identity() = SongIdentity(SourceId(sourceId), sourceKey)
}
