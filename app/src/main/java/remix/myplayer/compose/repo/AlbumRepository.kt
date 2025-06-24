package remix.myplayer.compose.repo

import android.content.Context
import android.provider.MediaStore.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.Album
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.PermissionUtil
import timber.log.Timber
import javax.inject.Inject

interface AlbumRepository {
  fun allAlbums(): List<Album>
}

class AlbumRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingPrefs: SettingPrefs
) : AlbumRepository, AbstractRepository(settingPrefs) {

  override fun allAlbums(): List<Album> {
    if (!PermissionUtil.hasNecessaryPermission()) {
      return emptyList()
    }
    val albumMaps: MutableMap<Long, MutableList<Album>> = LinkedHashMap()
    val albums: MutableList<Album> = ArrayList()
    val sortOrder = settingPrefs.albumSortOrder
    try {
      context.contentResolver
        .query(
          Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(
            Audio.Media.ALBUM_ID,
            Audio.Media.ALBUM,
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
                val albumId = cursor.getLong(0)
                if (albumMaps[albumId] == null) {
                  albumMaps[albumId] = ArrayList()
                }
                albumMaps[albumId]?.add(
                  Album(
                    albumId,
                    cursor.getString(1),
                    cursor.getLong(2),
                    cursor.getString(3),
                    0
                  )
                )
              } catch (ignored: Exception) {
              }
            }
            for ((_, value) in albumMaps) {
              try {
                val album = value[0]
                album.count = value.size
                albums.add(album)
              } catch (e: Exception) {
                Timber.v("addAlbum failed: $e")
              }
            }
          }
        }
    } catch (e: Exception) {
      Timber.v("getAllAlbum failed: $e")
    }
    return if (forceSort) {
      ItemsSorter.sortedAlbums(albums, sortOrder)
    } else {
      albums
    }
  }
}