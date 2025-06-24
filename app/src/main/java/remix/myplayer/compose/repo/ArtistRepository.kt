package remix.myplayer.compose.repo

import android.content.Context
import android.provider.MediaStore.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.PermissionUtil
import timber.log.Timber
import javax.inject.Inject

interface ArtistRepository {
  fun allArtists(): List<Artist>
}

class ArtistRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingPrefs: SettingPrefs
) : ArtistRepository, AbstractRepository(settingPrefs) {

  override fun allArtists(): List<Artist> {
    if (!PermissionUtil.hasNecessaryPermission()) {
      return emptyList()
    }

    val artistMaps: MutableMap<Long, MutableList<Artist>> = LinkedHashMap()
    val artists: MutableList<Artist> = ArrayList()
    val sortOrder = settingPrefs.artistSortOrder
    try {
      context.contentResolver
        .query(
          Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(
            Audio.Media.ARTIST_ID,
            Audio.Media.ARTIST
          ),
          baseSelection,
          baseSelectionArgs,
          sortOrder
        ).use { cursor ->
          if (cursor != null) {
            while (cursor.moveToNext()) {
              try {
                val artistId = cursor.getLong(0)
                if (artistMaps[artistId] == null) {
                  artistMaps[artistId] = ArrayList()
                }
                artistMaps[artistId]?.add(Artist(artistId, cursor.getString(1), 0))
              } catch (ignored: Exception) {
              }
            }
            for ((_, value) in artistMaps) {
              try {
                val artist = value[0]
                artist.count = value.size
                artists.add(artist)
              } catch (e: Exception) {
                Timber.v("addArtist failed: $e")
              }
            }
          }
        }
    } catch (e: Exception) {
      Timber.v("getAllArtist failed: $e")
    }
    return if (forceSort) {
      ItemsSorter.sortedArtists(artists, sortOrder)
    } else {
      artists
    }
  }

}