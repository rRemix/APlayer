package remix.myplayer.coil

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import coil3.request.ImageRequest
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import okio.ByteString.Companion.encodeUtf8
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.model.lastfm.Image
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_COVER_ALWAYS
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_COVER_WIFI_ONLY
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_LASTFM
import remix.myplayer.lyric.provider.SearchScorer
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.SongQuery
import remix.myplayer.repo.GenreRepository
import remix.myplayer.repo.MusicSourceRepository
import remix.myplayer.repo.SongMetadataRepository
import remix.myplayer.request.netease.NetEaseClient
import remix.myplayer.request.network.LastFMApi
import remix.myplayer.util.SearchKeyUtil
import remix.myplayer.util.Util
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by Remix on 2021/4/20
 */
@Singleton
class CoverUriResolver @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val neClient: NetEaseClient,
  private val lastFMApi: LastFMApi,
  private val settingPrefs: SettingPrefs,
  private val coverPrefs: CoverPrefs,
  private val songRepo: SongRepository,
  private val songMetadataRepository: SongMetadataRepository,
  private val musicSources: MusicSourceRepository,
  private val playListDao: PlayListDao,
  private val genres: GenreRepository
) {

  class CoverCandidate(val network: Boolean = false, private val load: suspend () -> Uri) {
    suspend fun fetch(): Uri = load()
  }

  data class CoverSources(
    val diskKey: String,
    val candidates: List<CoverCandidate>
  )

  val revisions = coverPrefs.revisions

  fun memoryRevision(model: APlayerModel): String = coverPrefs.memoryRevision(cacheKey(model))

  fun observeMemoryRevision(model: APlayerModel) = coverPrefs.observeMemoryRevision(cacheKey(model))

  fun invalidate(key: String) = coverPrefs.invalidate(key)

  fun invalidateAll() = coverPrefs.invalidateAll()

  fun refreshMemory() = coverPrefs.refreshMemory()

  fun request(model: APlayerModel, memoryVersion: String = memoryRevision(model)): ImageRequest.Builder {
    val keyParts = when (model) {
      is Song -> songCacheKeyParts(model)
      is Album -> listOf(model.album, model.artist, model.count)
      is Artist -> listOf(model.artist, model.count)
      is PlayList -> model.songIdentities.toList()
      is Genre -> listOf(model.genre, model.count)
      else -> error("Unsupported cover model")
    }
    val embeddedCoverFlags = when (model) {
      is Song -> listOf(shouldLoadEmbeddedCover(model))
      is PlayList -> buildList {
        if (model.songIdentities.any { it.sourceId == SourceId.MEDIASTORE }) {
          add(settingPrefs.loadLocalEmbeddedCover)
        }
        if (model.songIdentities.any { it.sourceId.isRemote }) {
          add(settingPrefs.loadRemoteEmbeddedCover)
        }
      }
      else -> listOf(settingPrefs.loadLocalEmbeddedCover)
    }
    // Auto-download permission is intentionally excluded so existing covers still work offline.
    val sourceHash = hashKeyParts(keyParts + embeddedCoverFlags + settingPrefs.downloadSource)
    val key = cacheKey(model)
    val diskVersion = coverPrefs.diskRevision(key)
    val diskKey = "cover:${key.encodeUtf8().sha256().hex()}:$diskVersion:$sourceHash"
    return ImageRequest.Builder(context)
      .data(model)
      .memoryCacheKey("cover:$key:$memoryVersion:$sourceHash")
      .diskCacheKey(diskKey)
  }

  fun cacheKey(model: Any): String = when (model) {
    is Song -> "song:${model.getKey()}"
    is Album -> "album:${model.albumId}"
    is Artist -> "artist:${model.artistId}"
    is PlayList -> "playlist:${model.id}"
    is Genre -> "genre:${model.id}"
    // Folder headers observe revisions, but load their individual song covers.
    is Folder -> "folder:${model.path}"
    else -> error("unknown model")
  }

  // Only called when Coil misses the memory cache. For collections, the disk key also
  // depends on the actual songs being tried.
  suspend fun resolveSources(model: APlayerModel, diskKey: String): CoverSources {
    checkWorkerThread()
    if (model is Song) return CoverSources(diskKey, fetchCandidates(model))

    val songs = when (model) {
      is Album -> songRepo.getSongs(SongQuery(albumId = model.albumId))
      is Artist -> songRepo.getSongs(SongQuery(artistId = model.artistId))
      is PlayList -> playListDao.observeById(model.id).first()?.let {
        songRepo.getSongs(listOf(it.toModel()))
      }.orEmpty()
      is Genre -> genres.getSongs(model.id)
      else -> error("Unsupported cover model")
    }.take(MAX_CANDIDATE_SONGS)

    // Keep each song's embedded, MediaStore (local only), then online candidates together.
    // A successful candidate stops resolution in CoverInterceptor before the next song.
    val candidates = buildList {
      for (song in songs) addAll(fetchCandidates(song))
      when (model) {
        is Album -> {
          add(CoverCandidate { model.artUri })
          add(CoverCandidate(network = true) { fetchNetwork(model) })
        }
        is Artist -> add(CoverCandidate(network = true) { fetchNetwork(model) })
        else -> Unit // Playlists and genres have no collection-level artwork source.
      }
    }

    val songsHash = hashKeyParts(songs.flatMap(::songCacheKeyParts))
    return CoverSources("$diskKey:$songsHash", candidates)
  }

  private fun songCacheKeyParts(song: Song): List<Any> = if (song.isRemote()) {
    // The first embedded read publishes tags. Those tags must not change the artwork
    // key and force an immediate second remote file open on the next list refresh.
    listOf(song.songIdentity, song.data, song.dateModified, song.size)
  } else {
    listOf(song.songIdentity, song.data, song.albumId, song.title, song.album, song.artist,
      song.dateModified, song.size)
  }

  private fun hashKeyParts(parts: List<Any>): String = buildString {
    for (part in parts) {
      val value = part.toString()
      append(value.length).append(':').append(value)
    }
  }.encodeUtf8().sha256().hex()

  private fun localCandidates(song: Song): List<CoverCandidate> = buildList {
    if (shouldLoadEmbeddedCover(song) && song.data.isNotEmpty()) {
      add(CoverCandidate { embeddedUri(song) })
    }
    // Album art provider only knows local MediaStore albums.
    if (song.isLocal()) add(CoverCandidate { song.artUri })
  }

  private fun fetchCandidates(song: Song): List<CoverCandidate> =
    localCandidates(song) + CoverCandidate(network = true) { fetchNetwork(song) }

  private suspend fun fetchNetwork(initialModel: APlayerModel): Uri {
    if (!canDownloadCover()) return Uri.EMPTY

    return try {
      val model = if (initialModel is Song && initialModel.isRemote()) {
        songMetadataRepository.getOrFetch(initialModel)
      } else initialModel
      val url = if (downloadFromLastFM()) fetchLastFM(model) else fetchNetEase(model)
      url?.takeIf { it.isNotEmpty() }?.toUri() ?: Uri.EMPTY
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.v(e)
      Uri.EMPTY
    }
  }

  private suspend fun fetchLastFM(model: APlayerModel): String? {
    val images = when (model) {
      is Song -> lastFMApi.searchLastFMAlbum(model.album, model.artist, null).album?.image
      is Album -> lastFMApi.searchLastFMAlbum(model.album, model.artist, null).album?.image
      is Artist -> lastFMApi.searchLastFMArtist(model.artist, null).artist?.image
      else -> return null
    }
    return getLargestImageUrl(images)
  }

  private suspend fun fetchNetEase(model: APlayerModel): String? {
    for (key in SearchKeyUtil.getSearchKeys(model).take(CANDIDATE_KEY_NUMBER)) {
      val url = when (model) {
        is Song -> neClient.searchSongList(key.value)
          .map { ne ->
            ne to SearchScorer.calculateSongScoreWithKeyKind(
              targetSong = model,
              candidateTitle = ne.name,
              candidateArtist = ne.ar?.joinToString(", ") { it.name ?: "" },
              candidateAlbum = ne.al?.name,
              candidateDuration = ne.dt,
              keyword = key.value,
              keyKind = key.kind
            )
          }
          .filter { it.second.isValid }
          .maxByOrNull { it.second.score }
          ?.first?.al?.picUrl
        is Album -> neClient.searchAlbumList(key.value)
          .map { ne -> ne to SearchScorer.calculateAlbumScore(model, ne.name, null) }
          .filter { it.second.isValid }
          .maxByOrNull { it.second.score }
          ?.first?.picUrl
        is Artist -> neClient.searchArtistList(key.value)
          .map { ne -> ne to SearchScorer.calculateArtistScore(model, ne.name) }
          .filter { it.second.isValid }
          .maxByOrNull { it.second.score }
          ?.first?.picUrl
        else -> return null
      }
      if (!url.isNullOrEmpty()) return url
    }
    return null
  }

  private fun embeddedUri(path: String): Uri =
    Uri.Builder().scheme(SCHEME_EMBEDDED).path(path).build()

  /**
   * Remote artwork cannot be addressed by path alone: the resolved locator carries the
   * credentials, so it travels with the request instead of being stored on [Song].
   */
  private suspend fun embeddedUri(song: Song): Uri {
    if (song.isLocal()) return embeddedUri(song.data)
    val source = musicSources.resolve(song)
    return Uri.Builder()
      .scheme(SCHEME_EMBEDDED)
      .authority(AUTHORITY_REMOTE)
      .appendQueryParameter(PARAM_LOCATOR, source.locator)
      .appendQueryParameter(PARAM_AUTHORIZATION, source.headers[AUTHORIZATION].orEmpty())
      .appendQueryParameter(PARAM_SOURCE_ID, song.sourceId.value)
      .appendQueryParameter(PARAM_SOURCE_KEY, song.sourceKey)
      .appendQueryParameter(PARAM_SIZE, song.size.toString())
      .appendQueryParameter(PARAM_MODIFIED, song.dateModified.toString())
      .build()
  }

  private fun shouldLoadEmbeddedCover(song: Song): Boolean =
    if (song.isRemote()) settingPrefs.loadRemoteEmbeddedCover else settingPrefs.loadLocalEmbeddedCover

  private fun downloadFromLastFM() = settingPrefs.downloadSource == DOWNLOAD_LASTFM

  private fun canDownloadCover(): Boolean {
    return when (settingPrefs.autoDownloadCover) {
      DOWNLOAD_COVER_ALWAYS -> true
      DOWNLOAD_COVER_WIFI_ONLY -> Util.isWifi(context)
      else -> false
    }
  }

  private fun getLargestImageUrl(images: List<Image>?): String? {
    val imageUrls = images.orEmpty().associate { (it.size?.uppercase() ?: "UNKNOWN") to it.text }
    val size = LASTFM_IMAGE_SIZES.firstOrNull { it in imageUrls } ?: return null
    return imageUrls[size]
  }

  companion object {

    private val LASTFM_IMAGE_SIZES = listOf("MEGA", "EXTRALARGE", "LARGE", "MEDIUM", "SMALL", "UNKNOWN")

    private const val CANDIDATE_KEY_NUMBER = 1

    private const val MAX_CANDIDATE_SONGS = 5

    const val SCHEME_EMBEDDED = "embedded"

    /** Query parameters carried by a remote [SCHEME_EMBEDDED] request. */
    const val PARAM_LOCATOR = "locator"
    const val PARAM_AUTHORIZATION = "authorization"
    const val PARAM_SOURCE_ID = "sourceId"
    const val PARAM_SOURCE_KEY = "sourceKey"
    const val PARAM_SIZE = "size"
    const val PARAM_MODIFIED = "modified"

    private const val AUTHORITY_REMOTE = "remote"
    private const val AUTHORIZATION = "Authorization"
  }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoverUriResolverEntryPoint {

  fun coverUriResolver(): CoverUriResolver
}

fun Context.coverUriResolver(): CoverUriResolver =
  EntryPointAccessors.fromApplication(applicationContext, CoverUriResolverEntryPoint::class.java)
    .coverUriResolver()
