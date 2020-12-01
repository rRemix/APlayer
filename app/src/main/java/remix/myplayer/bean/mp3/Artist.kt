package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2017/10/22.
 */

data class Artist(val artistID: Long,
                  val artist: String,
                  var count: Int){
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Artist

    if (artistID != other.artistID) return false

    return true
  }

  override fun hashCode(): Int {
    return artistID.hashCode()
  }
}
