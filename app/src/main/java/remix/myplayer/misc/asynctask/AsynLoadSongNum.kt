package remix.myplayer.misc.asynctask

import android.database.Cursor
import android.os.AsyncTask
import android.provider.MediaStore
import remix.myplayer.App.Companion.context
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil.baseSelection
import remix.myplayer.util.Util

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/11 14:07
 */
@Deprecated("")
abstract class AsynLoadSongNum(private val type: Int) : AsyncTask<Int, Int, Int>() {

  override fun doInBackground(vararg params: Int?): Int {
    val resolver = context.contentResolver
    val isAlbum = type == Constants.ALBUM
    var cursor: Cursor? = null
    try {
      cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID),
          baseSelection
              + " and " + (if (isAlbum) MediaStore.Audio.Media.ALBUM_ID else MediaStore.Audio.Media.ARTIST_ID) + "=" + params[0],
          null, null)
      return cursor?.count ?: 0
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      Util.closeSafely(cursor)
    }
    return 0
  }
}