package remix.myplayer.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.observe
import remix.myplayer.helper.ItemsSorter
import javax.inject.Inject

interface AlbumRepository { fun observeAlbums(): Flow<List<Album>> }

class AlbumRepoImpl @Inject constructor(
  private val songs: SongRepository,
  private val settings: SettingPrefs
) : AlbumRepository {
  override fun observeAlbums(): Flow<List<Album>> = songs.observeSongs().map { songs ->
    songs.groupBy { it.albumId }.map { (id, tracks) ->
      val song = tracks.first()
      Album(id, song.album, song.artistId, song.artist, tracks.size)
    }
  }.combine(settings.observe(PrefKeys.Setting.ALBUM_SORT_ORDER) { albumSortOrder }) { items, order ->
    ItemsSorter.sortedAlbums(items, order)
  }.flowOn(Dispatchers.Default)
}
