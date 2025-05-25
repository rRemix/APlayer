package remix.myplayer.compose.repo

import android.provider.MediaStore.Audio
import remix.myplayer.compose.prefs.Setting

abstract class AbstractRepository(private val setting: Setting) {
  protected val forceSort by lazy {
    setting.forceSort
  }

  val baseSelection: String
    get() {
      val deleteIds = setting.deleteIds
      val blacklist = setting.blacklist
      val baseSelection = " _data != '' AND " + Audio.Media.SIZE + " > " + setting.scanSize
      if (deleteIds.isEmpty() && blacklist.isEmpty()) {
        return baseSelection
      }
      val builder = StringBuilder(baseSelection)
      var i = 0
      if (deleteIds.isNotEmpty()) {
        builder.append(" AND ")
        for (id in deleteIds) {
          if (i == 0) {
            builder.append(Audio.Media._ID).append(" not in (")
          }
          builder.append(id)
          builder.append(if (i != deleteIds.size - 1) "," else ")")
          i++
        }
      }
      if (blacklist.isNotEmpty()) {
        builder.append(" AND ")
        i = 0
        for (path in blacklist) {
          builder.append(Audio.Media.DATA + " NOT LIKE ").append(" ? ")
          builder.append(if (i != blacklist.size - 1) " AND " else "")
          i++
        }
      }
      return builder.toString()
    }

  val baseSelectionArgs: Array<String?>
    get() {
      val blacklist = setting.blacklist
      val selectionArgs = arrayOfNulls<String>(blacklist.size)
      val iterator: Iterator<String> = blacklist.iterator()
      var i = 0
      while (iterator.hasNext()) {
        selectionArgs[i] = iterator.next() + "%"
        i++
      }
      return selectionArgs
    }
}