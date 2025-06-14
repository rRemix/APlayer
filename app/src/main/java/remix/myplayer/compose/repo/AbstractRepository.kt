package remix.myplayer.compose.repo

import android.database.Cursor
import android.os.Build
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns
import remix.myplayer.bean.mp3.Song
import remix.myplayer.bean.mp3.Song.Companion.EMPTY_SONG
import remix.myplayer.compose.prefs.Setting
import remix.myplayer.util.Util

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

  val baseProjection: Array<String> = run {
    val projection = ArrayList(
      listOf(
        AudioColumns._ID,
        AudioColumns.TITLE,
        AudioColumns.TITLE_KEY,
        AudioColumns.DISPLAY_NAME,
        AudioColumns.TRACK,
        AudioColumns.SIZE,
        AudioColumns.YEAR,
        AudioColumns.TRACK,
        AudioColumns.DURATION,
        AudioColumns.DATE_MODIFIED,
        AudioColumns.DATE_ADDED,
        AudioColumns.DATA,
        AudioColumns.ALBUM_ID,
        AudioColumns.ALBUM,
        AudioColumns.ARTIST_ID,
        AudioColumns.ARTIST
      )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      projection.add(AudioColumns.GENRE)
    }
    projection.toTypedArray()
  }

  fun resolveSong(cursor: Cursor?): Song {
    if (cursor == null || cursor.columnCount <= 0) {
      return EMPTY_SONG
    }
    return Song.Local(
      id = cursor.getLong(cursor.getColumnIndex(AudioColumns._ID)),
      displayName = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.DISPLAY_NAME)),
        Util.TYPE_DISPLAYNAME
      ),
      title = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.TITLE)),
        Util.TYPE_SONG
      ),
      album = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.ALBUM)),
        Util.TYPE_ALBUM
      ),
      albumId = cursor.getLong(cursor.getColumnIndex(AudioColumns.ALBUM_ID)),
      artist = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.ARTIST)),
        Util.TYPE_ARTIST
      ),
      artistId = cursor.getLong(cursor.getColumnIndex(AudioColumns.ARTIST_ID)),
      _duration = cursor.getLong(cursor.getColumnIndex(AudioColumns.DURATION)),
      data = cursor.getString(cursor.getColumnIndex(AudioColumns.DATA)),
      size = cursor.getLong(cursor.getColumnIndex(AudioColumns.SIZE)),
      year = cursor.getLong(cursor.getColumnIndex(AudioColumns.YEAR)).toString(),
      _genre = cursor.getColumnIndex(AudioColumns.GENRE).let {
        if (it != -1) {
          cursor.getString(it)
        } else {
          null
        }
      },
      track = cursor.getLong(cursor.getColumnIndex(AudioColumns.TRACK)).toString(),
      dateModified = cursor.getLong(cursor.getColumnIndex(AudioColumns.DATE_MODIFIED))
    )
  }
}