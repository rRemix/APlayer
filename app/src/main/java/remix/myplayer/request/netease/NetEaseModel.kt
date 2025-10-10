package remix.myplayer.request.netease

import kotlinx.serialization.Serializable

@Serializable
data class NetEaseSong(
  val id: Long,
  val name: String?,
  // 艺术家
  val ar: List<AR>?,
  // 专辑
  val al: AL?,
  // 时长
  val dt: Long
) {

  @Serializable
  data class AL(
    val id: Long,
    val name: String?,
    val picUrl: String?
  )

  @Serializable
  data class AR(
    val id: Long,
    val name: String?,
//    val alias: List<String>?
  )
}

@Serializable
data class NetEaseAlbum(
  val id: Long,
  val name: String?,
  val picUrl: String?
)

@Serializable
data class NetEaseArtist(
  val id: Long,
  val name: String?,
  val picUrl: String?
)