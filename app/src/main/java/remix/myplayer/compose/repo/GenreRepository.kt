package remix.myplayer.compose.repo

import android.content.Context
import android.provider.MediaStore.Audio.Genres
import com.tencent.bugly.crashreport.CrashReport
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.util.PermissionUtil
import javax.inject.Inject

interface GenreRepository {
  fun allGenres(): List<Genre>
}

class GenreRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingPrefs: SettingPrefs
) : GenreRepository, AbstractRepository(settingPrefs) {

  override fun allGenres(): List<Genre> {
    if (!PermissionUtil.hasNecessaryPermission()) {
      return emptyList()
    }

    val genres: MutableList<Genre> = ArrayList()
    try {
      context.contentResolver.query(
        Genres.EXTERNAL_CONTENT_URI,
        arrayOf(Genres._ID, Genres.NAME),
        null,
        null,
        settingPrefs.genreSortOrder
      )?.use { cursor ->
        while (cursor.moveToNext()) {
          val genreId = cursor.getLong(0)
          if (genreId > 0) {
            val songs = getSongsByGenreId(genreId)
            genres.add(Genre(genreId, cursor.getString(1) ?: "", songs.size))
          }
        }
      }
    } catch (e: Exception) {
      CrashReport.postCatchedException(e)
    }
    return genres
  }

  private fun getSongsByGenreId(genreId: Long, sortOrder: String? = null): List<Song> {
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
    return songs
  }
}