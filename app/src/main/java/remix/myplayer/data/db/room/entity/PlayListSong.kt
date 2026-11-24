package remix.myplayer.data.db.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.model.audio.PlayList as DomainPlayList

@Entity(
  tableName = "playlist_tracks",
  primaryKeys = ["playlist_id", "source_id", "source_key"],
  foreignKeys = [ForeignKey(entity = PlayList::class, parentColumns = ["id"],
    childColumns = ["playlist_id"], onDelete = ForeignKey.CASCADE)],
  indices = [Index(value = ["playlist_id", "position"], unique = true)]
)
data class PlayListSong(
  @ColumnInfo(name = "playlist_id") val playlistId: Long,
  @ColumnInfo(name = "source_id") val sourceId: String,
  @ColumnInfo(name = "source_key") val sourceKey: String,
  val position: Int
)

data class PlayListWithSongs(
  @Embedded val playlist: PlayList,
  @Relation(parentColumn = "id", entityColumn = "playlist_id") val songs: List<PlayListSong>
) {
  fun toModel() = DomainPlayList(playlist.id, playlist.name,
    songs.sortedBy { it.position }.map { SongIdentity(SourceId(it.sourceId), it.sourceKey) }, playlist.date)
}
