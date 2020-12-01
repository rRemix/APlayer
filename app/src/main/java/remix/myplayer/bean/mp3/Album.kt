package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2017/10/22.
 */

data class Album(val albumID: Long,
                 val album: String,
                 val artistID: Long,
                 val artist: String,
                 var count: Int = 0){
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Album

    if (albumID != other.albumID) return false

    return true
  }

  override fun hashCode(): Int {
    return albumID.hashCode()
  }
}
