package remix.myplayer.request.qq

import kotlinx.serialization.Serializable

@Serializable
data class QQSong(
  val id: Long,
  val mid: String,
  val title: String,
  val subtitle: String?,
  val artist: List<String>,
  val album: String,
  val duration: Long,
  val language: Int
)

@Serializable
data class QQAlbum(
  val id: Long,
  val mid: String,
  val name: String,
  val picUrl: String?,
  val songCount: Int,
  val publishTime: Long,
  val author: String
)

@Serializable
data class QQArtist(
  val id: Long,
  val mid: String,
  val name: String,
  val picUrl: String?
)