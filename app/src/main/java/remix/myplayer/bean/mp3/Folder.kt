package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2018/1/9.
 */

data class Folder(val name: String?,
                  val count: Int,
                  val path: String) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Folder

    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }
}
