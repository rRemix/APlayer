package remix.myplayer.compose.repo

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.Setting
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.MediaStoreUtil.getSongInfo
import timber.log.Timber
import javax.inject.Inject

interface SongRepository {
  fun allSongs(): List<Song>
  fun getSongs(selection: String?, selectionValues: Array<String?>?, sortOrder: String?): List<Song>

  fun song(id: Long): Song
  fun makeSongCursor(
    selection: String?,
    selectionValues: Array<String?>?,
    sortOrder: String?
  ): Cursor?
}

class SongRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val setting: Setting
) : SongRepository, AbstractRepository(setting) {
  override fun allSongs(): List<Song> {
    return getSongs(
      null,
      null,
      setting.songSortOrder
    )
  }

  override fun getSongs(
    selection: String?, selectionValues: Array<String?>?,
    sortOrder: String?
  ): List<Song> {
    val songs: MutableList<Song> = ArrayList()
    try {
      makeSongCursor(selection, selectionValues, sortOrder).use { cursor ->
        if (cursor != null && cursor.count > 0) {
          while (cursor.moveToNext()) {
            songs.add(getSongInfo(cursor))
          }
        }
      }
    } catch (e: Exception) {
      Timber.v(e)
    }
    return if (forceSort) {
      ItemsSorter.sortedSongs(songs, sortOrder)
    } else {
      songs
    }
  }

  override fun makeSongCursor(
    selection: String?, selectionValues: Array<String?>?,
    sortOrder: String?
  ): Cursor? {
    var selection = selection
    var selectionValues = selectionValues
    selection = if (selection != null && selection.trim { it <= ' ' } != "") {
      "$selection AND ($baseSelection)"
    } else {
      baseSelection
    }
    if (selectionValues == null) {
      selectionValues = arrayOfNulls(0)
    }
    val baseSelectionArgs = baseSelectionArgs
    val newSelectionValues = arrayOfNulls<String>(selectionValues.size + baseSelectionArgs.size)
    System.arraycopy(selectionValues, 0, newSelectionValues, 0, selectionValues.size)
    if (newSelectionValues.size - selectionValues.size >= 0) {
      System.arraycopy(
        baseSelectionArgs, 0,
        newSelectionValues, selectionValues.size,
        newSelectionValues.size - selectionValues.size
      )
    }

    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      Audio.Media.EXTERNAL_CONTENT_URI
    }

    return try {
      context.contentResolver.query(
        uri,
        baseProjection, selection, newSelectionValues, sortOrder
      )
    } catch (e: SecurityException) {
      null
    }
  }


  override fun song(id: Long): Song {
    TODO("Not yet implemented")
  }


  companion object {
    private val baseProjection: Array<String> = run {
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
  }
}