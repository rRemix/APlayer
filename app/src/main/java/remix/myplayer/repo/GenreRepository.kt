package remix.myplayer.repo

import android.content.Context
import android.provider.MediaStore.Audio.Genres
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.util.PermissionUtil
import timber.log.Timber
import javax.inject.Inject

interface GenreRepository {

  fun allGenres(): List<Genre>
}

class GenreRepoImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
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
            genres.add(Genre(genreId, cursor.getString(1) ?: "", count(genreId)))
          }
        }
      }
    } catch (e: Exception) {
      Timber.w(e)
    }
    return ItemsSorter.sortedGenres(genres, settingPrefs.genreSortOrder)
  }

  private fun count(genreId: Long): Int {
    return context.contentResolver.query(
      Genres.Members.getContentUri("external", genreId),
      arrayOf(Genres.Members.AUDIO_ID),
      null,
      null,
      null
    )?.use { it.count } ?: 0
  }
}
