package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import remix.myplayer.data.model.audio.SongMetadata

@Entity
data class MetaDataCache(
  @PrimaryKey
  val url: String,
  val title: String,
  val artist: String,
  val album: String,
  val duration: Long,
  val fileSize: Long,
  val lastModified: Long,
  val year: String,
  val genre: String,
  val track: String,
  val updateTime: Long = System.currentTimeMillis(),
  @ColumnInfo(defaultValue = "''") val bitRate: String = "",
  @ColumnInfo(defaultValue = "''") val sampleRate: String = "",
  // Existing rows contain extracted tags; directory-only rows explicitly set false.
  @ColumnInfo(defaultValue = "1") val tagsLoaded: Boolean = true
) {

  fun metadata() = SongMetadata(
    title, album, artist, duration, year, genre, track, bitRate, sampleRate
  )
}
