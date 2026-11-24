package remix.myplayer.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.observe
import remix.myplayer.helper.ItemsSorter
import javax.inject.Inject

interface ArtistRepository { fun observeArtists(): Flow<List<Artist>> }

class ArtistRepoImpl @Inject constructor(
  private val songs: SongRepository,
  private val settings: SettingPrefs
) : ArtistRepository {
  override fun observeArtists(): Flow<List<Artist>> = songs.observeSongs().map { songs ->
    songs.groupBy { it.artistId }.map { (id, tracks) ->
      val song = tracks.first()
      Artist(id, song.artist, tracks.size)
    }
  }.combine(settings.observe(PrefKeys.Setting.ARTIST_SORT_ORDER) { artistSortOrder }) { items, order ->
    ItemsSorter.sortedArtists(items, order)
  }.flowOn(Dispatchers.Default)
}
