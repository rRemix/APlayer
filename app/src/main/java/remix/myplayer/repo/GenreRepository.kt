package remix.myplayer.repo

import android.content.Context
import android.provider.MediaStore.Audio.Genres
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.observe
import remix.myplayer.helper.ItemsSorter
import timber.log.Timber
import javax.inject.Inject

interface GenreRepository {
  fun observeGenres(): Flow<List<Genre>>
  fun observeSongs(genreId: Long, sortOrder: String? = null): Flow<List<Song>>
  suspend fun getSongs(genreId: Long, sortOrder: String? = null): List<Song>
}

class GenreRepoImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val library: MediaStoreLibrary,
  private val settings: SettingPrefs
) : GenreRepository {
  // Android's genre membership is authoritative; resolve IDs against the filtered library.
  override fun observeGenres(): Flow<List<Genre>> = observeLocalSongs().map { snapshot ->
    try {
      context.contentResolver.query(Genres.EXTERNAL_CONTENT_URI, arrayOf(Genres._ID, Genres.NAME),
        null, null, null)?.use { cursor ->
        buildList {
          while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            if (id > 0) {
              val count = members(id, snapshot).size
              if (count > 0) add(Genre(id, cursor.getString(1).orEmpty(), count))
            }
          }
        }
      }.orEmpty()
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Timber.w(error, "Unable to query genres")
      emptyList()
    }
  }.combine(settings.observe(PrefKeys.Setting.GENRE_SORT_ORDER) { genreSortOrder }) { items, order ->
    ItemsSorter.sortedGenres(items, order)
  }.flowOn(Dispatchers.IO)

  override fun observeSongs(genreId: Long, sortOrder: String?): Flow<List<Song>> = observeLocalSongs()
    .map { ItemsSorter.sortedSongs(members(genreId, it), sortOrder) }.flowOn(Dispatchers.IO)

  override suspend fun getSongs(genreId: Long, sortOrder: String?): List<Song> = withContext(Dispatchers.IO) {
    library.ensureLoaded()
    ItemsSorter.sortedSongs(members(genreId, library.snapshots.value.songs), sortOrder)
  }

  private fun observeLocalSongs(): Flow<List<Song>> = library.snapshots
    .onStart { library.ensureLoaded() }
    .map { it.songs }

  private fun members(genreId: Long, snapshot: List<Song>): List<Song> = try {
    val byId = snapshot.mapNotNull { song -> song.mediaStoreId?.let { it to song } }.toMap()
    context.contentResolver.query(Genres.Members.getContentUri("external", genreId),
      arrayOf(Genres.Members.AUDIO_ID), null, null, null)?.use { cursor ->
      buildList { while (cursor.moveToNext()) byId[cursor.getLong(0)]?.let(::add) }
    }.orEmpty().distinctBy { it.songIdentity }
  } catch (error: CancellationException) {
    throw error
  } catch (error: Exception) {
    Timber.w(error, "Unable to query genre members")
    emptyList()
  }
}
