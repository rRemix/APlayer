package remix.myplayer.repo

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.Genres
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject


interface SongRepository {

  fun allSongs(): List<Song>

  fun getSongs(
    selection: String?,
    selectionValues: Array<String?>?,
    sortOrder: String? = null
  ): List<Song>

  fun song(id: Long): Song?

  suspend fun getSongsByModels(models: List<APlayerModel>): List<Song>

  fun getSongsByGenreId(genreId: Long, sortOrder: String? = null): List<Song>

  fun getLastAddedSongs(): List<Song>

  fun makeSongCursor(
    selection: String?,
    selectionValues: Array<String?>?,
    sortOrder: String?
  ): Cursor?
}

class SongRepoImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val playListDao: PlayListDao,
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
    checkWorkerThread()
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
    return ItemsSorter.sortedSongs(songs, sortOrder)
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

  override suspend fun getSongsByModels(models: List<APlayerModel>): List<Song> {
    checkWorkerThread()
    val result = arrayListOf<Song>()

    models.forEach {
      when (it) {
        is Song -> {
          result.add(it)
        }

        is Album -> {
          result.addAll(
            getSongs(
              Audio.Media.ALBUM_ID + "=?",
              arrayOf(it.albumID.toString()),
              settingPrefs.albumDetailSortOrder
            )
          )
        }

        is Artist -> {
          result.addAll(
            getSongs(
              Audio.Media.ARTIST_ID + "=?",
              arrayOf(it.artistID.toString()),
              settingPrefs.artistDetailSortOrder
            )
          )
        }

        is Genre -> {
          result.addAll(getSongsByGenreId(it.id, settingPrefs.genreDetailSortOrder))
        }

        is Folder -> {
          result.addAll(getSongs(null, null, settingPrefs.folderDetailSortOrder).filter { song ->
            song.data.substringBeforeLast("/", missingDelimiterValue = "") == it.path
          })
        }

        is PlayList -> {
          val playListSortOrder = settingPrefs.getPlayListDetailSortOrder(it.id)
          val customSort = playListSortOrder == SortOrder.PLAYLIST_SONG_CUSTOM
          val ids = it.audioIds.toList()

          val songs = getSongs(
            makeInStrQuery(ids),
            null,
            if (customSort) null else playListSortOrder
          )

          val tempArray: Array<Song> = Array(ids.size) { Song.EMPTY_SONG }
          songs.forEachIndexed { index, song ->
            tempArray[if (customSort) ids.indexOf(song.id) else index] = song
          }

          // remove no longer exist
          if (songs.size < ids.size) {
            val deleteIds = ArrayList<Long>()
            val existIds = songs.map { it.id }

            for (audioId in ids) {
              if (!existIds.contains(audioId)) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              it.audioIds.removeAll(deleteIds)
              playListDao.update(it)
            }
          }

          result.addAll(
            tempArray
              .filter { it.id != Song.EMPTY_SONG.id })
        }
      }
    }

    return result
  }

  override fun getSongsByGenreId(genreId: Long, sortOrder: String?): List<Song> {
    checkWorkerThread()
    val songs = ArrayList<Song>()
    context.contentResolver.query(
      Genres.Members.getContentUri("external", genreId),
      baseProjection,
      null,
      null,
      sortOrder
    )?.use { songCursor ->
      while (songCursor.moveToNext()) {
        songs.add(resolveSong(songCursor))
      }
    }
    return ItemsSorter.sortedSongs(songs, sortOrder)
  }

  override fun getLastAddedSongs(): List<Song> {
    checkWorkerThread()
    val today = Calendar.getInstance()
    today.time = Date()
    return getSongs(
      Audio.Media.DATE_ADDED + " >= ?",
      arrayOf((today.timeInMillis / 1000 - 3600 * 24 * 7).toString()),
      null
    )
  }
}
