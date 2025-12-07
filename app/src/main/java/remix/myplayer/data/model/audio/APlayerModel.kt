package remix.myplayer.data.model.audio

import remix.myplayer.data.db.room.entity.PlayList
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