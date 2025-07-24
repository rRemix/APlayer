package remix.myplayer.bean.mp3

import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.getSongIds
import remix.myplayer.util.Constants
import java.io.Serializable

/**
 * created by Remix on 2021/4/30
 */

interface APlayerModel : Serializable {

  fun getKey(): String
}

fun APlayerModel.type(): Int {
  return when (this) {
    is Album -> Constants.ALBUM
    is Artist -> Constants.ARTIST
    is PlayList -> Constants.PLAYLIST
    is Genre -> Constants.GENRE
    is Folder -> Constants.FOLDER
    else -> throw IllegalArgumentException("unknown model: $this")
  }
}

fun APlayerModel.songIds(): List<Long> {
  return when (this) {
    is Album -> this.getSongIds()
    is Artist -> this.getSongIds()
    is PlayList -> this.audioIds.toList()
    is Genre -> this.getSongIds()
    is Folder -> this.getSongIds()
    else -> throw IllegalArgumentException("unknown model: $this")
  }
}