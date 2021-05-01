package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.net.Uri

/**
 * Created by Remix on 2017/10/22.
 */

data class Album(val albumID: Long,
                 val album: String,
                 val artistID: Long,
                 val artist: String,
                 var count: Int = 0) : APlayerModel{
  val artUri: Uri
    get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumID)
}
