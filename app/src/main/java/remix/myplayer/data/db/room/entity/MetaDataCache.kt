package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import remix.myplayer.data.model.audio.Song

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
  val updateTime: Long = System.currentTimeMillis()
) {

  fun toRemoteSong(account: String, pwd: String): Song.Remote {
    return Song.Remote(
      title = title,
      album = album,
      artist = artist,
      duration = duration,
      data = url,
      size = fileSize,
      year = year,
      genre = genre,
      track = track,
      dateModified = lastModified,
      account = account,
      pwd = pwd
    ).apply {
      // 标记为已获取完成
      metaFetchState.set(2)
    }
  }
}