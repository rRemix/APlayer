package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2018/1/9.
 */

data class Folder(val name: String?, val count: Int, val path: String?, val parentId: Int) {


    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is Folder -> false
            else -> {
                this.parentId == other.parentId
            }
        }
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + count
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (parentId ?: 0)
        return result
    }
}
