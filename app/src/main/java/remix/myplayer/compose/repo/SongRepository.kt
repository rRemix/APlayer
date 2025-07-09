package remix.myplayer.compose.repo

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.checkWorkerThread
import remix.myplayer.util.ItemsSorter
import timber.log.Timber
import javax.inject.Inject

interface SongRepository {

  fun allSongs(): List<Song>

  fun getSongs(selection: String?, selectionValues: Array<String?>?, sortOrder: String?): List<Song>

  fun song(id: Long): Song?

  fun getSongsByModels(models: List<APlayerModel>): List<Song>

  fun makeSongCursor(
    selection: String?,
    selectionValues: Array<String?>?,
    sortOrder: String?
  ): Cursor?
}

class SongRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingPrefs: SettingPrefs
) : SongRepository, AbstractRepository(settingPrefs) {

  override fun allSongs(): List<Song> {
    return getSongs(
      null,
      null,
      settingPrefs.songSortOrder
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
            songs.add(resolveSong(cursor))
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

  override fun song(id: Long) =
    getSongs(Audio.Media._ID + "=?", arrayOf(id.toString() + ""), null).firstOrNull()

  override fun getSongsByModels(models: List<APlayerModel>): List<Song> {
    checkWorkerThread()
    val songs = arrayListOf<Song>()

    models.forEach {
      when (it) {
        is Song -> {
          songs.add(it)
        }

        is Album -> {
          songs.addAll(
            getSongs(
              Audio.Media.ALBUM_ID + "=?",
              arrayOf(it.albumID.toString()),
              settingPrefs.albumDetailSortOrder
            )
          )
        }

        is Artist -> {
          songs.addAll(
            getSongs(
              Audio.Media.ARTIST_ID + "=?",
              arrayOf(it.artistID.toString()),
              settingPrefs.artistDetailSortOrder
            )
          )
        }

        is Folder -> {
          songs.addAll(getSongs(null, null, settingPrefs.folderDetailSortOrder).filter { song ->
            song.data.substring(0, song.data.lastIndexOf("/")) == it.path
          })
        }

        is PlayList -> {
          songs.addAll(
            getSongs(
              Audio.Media._ID + " in(" + makeInStr(it.audioIds.toList()) + ")",
              null,
              null
            )
          )
        }
      }
    }

    return songs
  }
}