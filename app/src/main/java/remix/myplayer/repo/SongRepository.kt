package remix.myplayer.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.observe
import remix.myplayer.helper.ItemsSorter
import javax.inject.Inject

interface SongRepository {
  fun observeSongs(query: SongQuery = SongQuery()): Flow<List<Song>>
  /** Observe the current item; playlists follow their saved row and subsequent edits. */
  fun observeSongs(item: APlayerModel): Flow<List<Song>>
  suspend fun getSongs(query: SongQuery = SongQuery()): List<Song>
  /** Expand library items using the supplied playlist snapshot, including its saved track order. */
  suspend fun getSongs(items: List<APlayerModel>): List<Song>
  /** Resolve saved identities in the caller's order, retaining duplicate requests. No network I/O. */
  suspend fun resolveSongs(identities: List<SongIdentity>): List<Song>
}

class SongRepoImpl @Inject constructor(
  private val library: MediaStoreLibrary,
  private val sources: MusicSourceRepository,
  private val metadata: SongMetadataRepository,
  private val playlists: PlayListRepository,
  private val genres: GenreRepository,
  private val settings: SettingPrefs
) : SongRepository {
  override fun observeSongs(query: SongQuery): Flow<List<Song>> = library.snapshots
    .onStart { library.ensureLoaded() }
    .map { query.applyTo(it.songs) }
    .flowOn(Dispatchers.Default)

  override fun observeSongs(item: APlayerModel): Flow<List<Song>> {
    val tracks = when (item) {
      is Song -> flowOf(listOf(item))
      is PlayList -> combine(playlists.observePlayList(item.id), observeSongs(), metadata.revision,
        sources.observeSources()) { playlist, _, _, _ ->
        if (playlist == null) emptyList() else resolveSongs(playlist.songIdentities)
      }
      is Genre -> genres.observeSongs(item.id)
      else -> query(item)?.let(::observeSongs) ?: flowOf(emptyList())
    }
    return tracks.combine(observeSortOrder(item)) { items, order ->
      ItemsSorter.sortedSongs(items, order)
    }.flowOn(Dispatchers.Default)
  }

  override suspend fun getSongs(query: SongQuery): List<Song> = withContext(Dispatchers.Default) {
    library.ensureLoaded()
    query.applyTo(library.snapshots.value.songs)
  }

  override suspend fun getSongs(items: List<APlayerModel>): List<Song> = items.flatMap { item ->
    when (item) {
      is Song -> listOf(item)
      is PlayList -> ItemsSorter.sortedSongs(resolveSongs(item.songIdentities), sortOrder(item))
      is Genre -> genres.getSongs(item.id, sortOrder(item))
      else -> query(item, sortOrder(item))?.let { getSongs(it) }.orEmpty()
    }
  }

  override suspend fun resolveSongs(identities: List<SongIdentity>): List<Song> = withContext(Dispatchers.IO) {
    if (identities.isEmpty()) return@withContext emptyList()
    val requested = identities.toSet()
    if (requested.any { it.sourceId == SourceId.MEDIASTORE }) library.ensureLoaded()
    val found = library.snapshots.value.songs.filter { it.songIdentity in requested }
      .associateBy { it.songIdentity }.toMutableMap()
    val remoteSongs = mutableListOf<Song>()
    identities.filter { it.sourceId.isRemote }.distinct().groupBy { it.sourceId }.forEach { (id, keys) ->
      val source = sources.get(id) ?: return@forEach
      keys.forEach { key ->
        val name = key.sourceKey.substringAfterLast('/')
        remoteSongs += Song(id, key.sourceKey, RemotePaths.resolve(source.root, key.sourceKey), name,
          title = name.substringBeforeLast('.', name))
      }
    }
    metadata.withCachedMetadata(remoteSongs).forEach { found[it.songIdentity] = it }
    identities.mapNotNull(found::get)
  }

  private fun observeSortOrder(item: APlayerModel): Flow<String?> {
    val keys = when (item) {
      is Album -> arrayOf(PrefKeys.Setting.CHILD_ALBUM_SONG_SORT_ORDER)
      is Artist -> arrayOf(PrefKeys.Setting.CHILD_ARTIST_SONG_SORT_ORDER)
      is Folder -> arrayOf(PrefKeys.Setting.CHILD_FOLDER_SONG_SORT_ORDER)
      is Genre -> arrayOf(PrefKeys.Setting.CHILD_GENRE_SONG_SORT_ORDER)
      is PlayList -> arrayOf(PrefKeys.Setting.CHILD_PLAYLIST_SONG_SORT_ORDER,
        PrefKeys.Setting.CHILD_PLAYLIST_SONG_SORT_ORDER_PREFIX + item.id)
      else -> return flowOf(null)
    }
    return settings.observe(*keys) { sortOrder(item) }
  }

  private fun sortOrder(item: APlayerModel): String? = when (item) {
    is Album -> settings.albumDetailSortOrder
    is Artist -> settings.artistDetailSortOrder
    is Folder -> settings.folderDetailSortOrder
    is Genre -> settings.genreDetailSortOrder
    is PlayList -> settings.getPlayListDetailSortOrder(item.id)
    else -> null
  }

  private fun query(item: APlayerModel, order: String? = null): SongQuery? = when (item) {
    is Album -> SongQuery(albumId = item.albumId, sortOrder = order)
    is Artist -> SongQuery(artistId = item.artistId, sortOrder = order)
    is Folder -> SongQuery(folderPath = item.path, sortOrder = order)
    else -> null
  }
}
