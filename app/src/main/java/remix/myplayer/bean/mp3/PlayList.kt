package remix.myplayer.bean.mp3

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:22
 */
data class PlayList(val id: Int = 0,
                    val name: String,
                    val count: Int,
                    val date: Int) {



  override fun toString(): String {
    return "PlayList{" +
        "_Id=" + id +
        ", Name='" + name + '\''.toString() +
        ", Count=" + count +
        ", Date=" + date +
        '}'.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PlayList) return false

    if (id != other.id) return false
    if (name != other.name) return false
    if (count != other.count) return false
    if (date != other.date) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + name.hashCode()
    result = 31 * result + count
    result = 31 * result + date
    return result
  }
}
