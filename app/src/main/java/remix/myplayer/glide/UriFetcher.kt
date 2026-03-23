package remix.myplayer.glide

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Audio
import androidx.core.net.toUri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.lastfm.Image
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_COVER_ALWAYS
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_COVER_WIFI_ONLY
import remix.myplayer.data.prefs.SettingPrefs.Companion.DOWNLOAD_LASTFM
import remix.myplayer.lyric.provider.SearchScorer
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.usecase.FetchMetaDataUseCase
import remix.myplayer.request.netease.NetEaseClient
import remix.myplayer.request.network.LastFMApi
import remix.myplayer.util.Constants
import remix.myplayer.util.SearchKeyUtil
import remix.myplayer.util.Util
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by Remix on 2021/4/20
 */
@Singleton
class UriFetcher @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val neClient: NetEaseClient,
  private val lastFMApi: LastFMApi,
  private val settingPrefs: SettingPrefs,
  private val coverPrefs: CoverPrefs,
  private val songRepo: SongRepository,
  private val fetchMetaDataUseCase: FetchMetaDataUseCase
) {

  private var albumVersion = coverPrefs.getAlbumVersion()
    set(value) {
      field = value
      coverPrefs.putAlbumVersion(value)
    }
  private var artistVersion = coverPrefs.getArtistVersion()
    set(value) {
      field = value
      coverPrefs.putArtistVersion(value)
    }
  private var playListVersion = coverPrefs.getPlayListVersion()
    set(value) {
      field = value
      coverPrefs.putPlayListVersion(value)
    }

  fun cacheKey(model: Any): String = when (model) {
    is Song -> if (model.id > 0) "song:${model.id}" else "song-data:${model.data}"
    is Album -> "album:${model.albumID}"
    is Artist -> "artist:${model.artistID}"
    is PlayList -> "playlist:${model.id}"
    is Genre -> "genre:${model.id}"
    else -> error("unknown model")
  }

  fun fetch(model: Any): Uri {
    val uri = when (model) {
      is Song -> fetch(model)
      is Album -> fetch(model)
      is Artist -> fetch(model)
      is PlayList -> fetch(model)
      is Genre -> fetch(model)
      else -> throw IllegalArgumentException("unknown model: ${model::class.java.simpleName}")
    }

    if (uri != Uri.EMPTY) {
      Timber.v("uri: $uri")
    }

    return uri
  }

  fun updateAllVersion() {
    updateAlbumVersion()
    updateArtistVersion()
    updatePlayListVersion()
  }

  fun updateAlbumVersion() {
    albumVersion++
  }

  fun updateArtistVersion() {
    artistVersion++
  }

  fun updatePlayListVersion() {
    playListVersion++
  }

  fun clearAllCache() {
    coverPrefs.clearCoverUris()
  }

  private fun fetch(song: Song): Uri {
    checkWorkerThread()
    if (song is Song.Remote) {
      runBlocking {
        fetchMetaDataUseCase(song)
      }
    }
    if (song.isLocal()) { // 仅本地歌曲
      if (song.albumId <= 0 || song.id <= 0) {
        return Uri.EMPTY
      }
      // 自定义封面
//      val customArtFile = getCustomThumbIfExist(song.albumId, Constants.ALBUM)
//      if (customArtFile != null) {
//        return Uri.fromFile(customArtFile)
//      }

      // 内置
      if (ignoreMediaStore()) {
        val songs = songRepo.getSongs(Audio.Media._ID + "=" + song.id, null)
        if (songs.isNotEmpty()) {
          return (PREFIX_EMBEDDED + songs[0].data).toUri()
        }
      } else if (isAlbumThumbExistInMediaCache(song.artUri)) {
        return song.artUri
      }
    }

    // 网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMAlbum =
            runBlocking { lastFMApi.searchLastFMAlbum(song.album, song.artist, null) }
          val lastFMUri = getLargestImageUrl(lastFMAlbum.album?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val searchKeys = SearchKeyUtil.getSearchKeys(song)
          for (key in searchKeys.take(CANDIDATE_KEY_NUMBER)) {
            val candidates = neClient.searchSongList(key.value)
            val best = candidates
              .map { ne ->
                ne to SearchScorer.calculateSongScoreWithKeyKind(
                  targetSong = song,
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
              ?.first

            if (best?.al?.picUrl?.isNotEmpty() == true) {
              return best.al.picUrl.toUri()
            }
          }
        }
      } catch (e: Exception) {
        Timber.v(e)
      }
    }

    return Uri.EMPTY
  }

  private fun fetch(album: Album): Uri {
    // 自定义封面
//    val customArtFile = getCustomThumbIfExist(album.albumID, Constants.ALBUM)
//    if (customArtFile != null) {
//      return Uri.fromFile(customArtFile)
//    }

    // 内置
    if (ignoreMediaStore()) {
      val songs = songRepo.getSongs(Audio.Media.ALBUM_ID + "=" + album.albumID, null)
      if (songs.isNotEmpty()) {
        return (PREFIX_EMBEDDED + songs[0].data).toUri()
      }
    } else if (isAlbumThumbExistInMediaCache(album.artUri)) {
      return album.artUri
    }

    // 网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMAlbum =
            runBlocking { lastFMApi.searchLastFMAlbum(album.album, album.artist, null) }
          val lastFMUri = getLargestImageUrl(lastFMAlbum.album?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val searchKeys = SearchKeyUtil.getSearchKeys(album)
          for (key in searchKeys.take(CANDIDATE_KEY_NUMBER)) {
            val candidates = neClient.searchAlbumList(key.value)
            val best = candidates
              .map { ne ->
                ne to SearchScorer.calculateAlbumScore(
                  album,
                  ne.name,
                  null,
                )
              }
              .filter { it.second.isValid }
              .maxByOrNull { it.second.score }
              ?.first

            if (best?.picUrl?.isNotEmpty() == true) {
              return best.picUrl.toUri()
            }
          }
        }
      } catch (e: Exception) {
        Timber.v(e)
      }
    }

    return Uri.EMPTY
  }

  private fun fetch(artist: Artist): Uri {
    // 自定义封面
//    val customArtFile = getCustomThumbIfExist(artist.artistID, Constants.ARTIST)
//    if (customArtFile != null) {
//      return Uri.fromFile(customArtFile)
//    }

    // 内置
    val imageUrl = getArtistArt(artist.artistID)
    if (imageUrl.isNotEmpty()) {
      return imageUrl.toUri()
    }

    //网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMArtist = runBlocking { lastFMApi.searchLastFMArtist(artist.artist, null) }
          val lastFMUri = getLargestImageUrl(lastFMArtist.artist?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val searchKeys = SearchKeyUtil.getSearchKeys(artist)
          for (key in searchKeys.take(CANDIDATE_KEY_NUMBER)) {
            val candidates = neClient.searchArtistList(key.value)
            val best = candidates
              .map { ne ->
                ne to SearchScorer.calculateArtistScore(
                  artist,
                  ne.name,
                )
              }
              .filter { it.second.isValid }
              .maxByOrNull { it.second.score }
              ?.first

            if (best?.picUrl?.isNotEmpty() == true) {
              return best.picUrl.toUri()
            }
          }
        }
      } catch (e: Exception) {
        Timber.v(e)
      }
    }

    return Uri.EMPTY
  }

  private fun fetch(playList: PlayList): Uri {
    // 自定义封面
//    val customArtFile = getCustomThumbIfExist(playList.id, Constants.PLAYLIST)
//    if (customArtFile != null) {
//      return Uri.fromFile(customArtFile)
//    }

    val songs = runBlocking { songRepo.getSongsByModels(listOf(playList)) }

    var uri: Uri
    for (song in songs) {
      uri = fetch(song)
      if (uri != Uri.EMPTY) {
        return uri
      }
    }

    return Uri.EMPTY
  }

  private fun fetch(genre: Genre): Uri {
    val songs = songRepo.getSongsByGenreId(genre.id)

    var uri: Uri
    for (song in songs) {
      uri = fetch(song)
      if (uri != Uri.EMPTY) {
        return uri
      }
    }

    return Uri.EMPTY
  }

  private fun ignoreMediaStore() = settingPrefs.ignoreMediaStore

  private fun downloadFromLastFM() = settingPrefs.downloadSource == DOWNLOAD_LASTFM

  private fun canDownloadCover(): Boolean {
    return when (settingPrefs.autoDownloadCover) {
      DOWNLOAD_COVER_ALWAYS -> true
      DOWNLOAD_COVER_WIFI_ONLY -> Util.isWifi(context)
      else -> false
    }
  }

  /**
   * 根据artistId搜索MediaStore中是否存在封面
   */
  private fun getArtistArt(artistId: Long): String {
    val songs = songRepo.getSongs(Audio.Media.ARTIST_ID + " = " + artistId, null)
    if (!songs.isEmpty()) {
      for (song in songs) {
        val uri = ContentUris
          .withAppendedId(
            "content://media/external/audio/albumart/".toUri(),
            song.albumId
          )
        if (isAlbumThumbExistInMediaCache(uri)) {
          return uri.toString()
        }
      }
    }
    return ""
  }

  /**
   * 判断某专辑在本地数据库是否有封面
   */
  private fun isAlbumThumbExistInMediaCache(uri: Uri): Boolean {
    var exist = false
    try {
      context.contentResolver.openInputStream(uri).use { ignored ->
        exist = true
      }
    } catch (_: Exception) {
    }
    return exist
  }

  /**
   * 返回自定义的封面
   */
  private fun getCustomThumbIfExist(id: Long, type: Int): File? {
    val img = File(DiskCache.getDiskCacheDir(context, "thumbnail"), "$type-$id.jpg")
    if (img.exists()) {
      return img
    }
    return null
  }

  private fun getSearchKey(model: Any): String? {
    return SearchKeyUtil.getSearchKeys(model).firstOrNull()?.value
  }

  private enum class ImageSize {
    SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
  }

  private fun getLargestImageUrl(images: List<Image>?): String? {
    if (images.isNullOrEmpty()) {
      return null
    }
    val imageUrls = HashMap<ImageSize, String?>()
    for (image in images) {
      var size: ImageSize? = null
      val attribute = image.size
      if (attribute == null) {
        size = ImageSize.UNKNOWN
      } else {
        try {
          size = ImageSize.valueOf(attribute.uppercase())
        } catch (_: IllegalArgumentException) {
          // if they suddenly again introduce a new image size
        }
      }
      if (size != null) {
        imageUrls.put(size, image.text)
      }
    }
    return getLargestImageUrl(imageUrls)
  }

  private fun getLargestImageUrl(imageUrls: Map<ImageSize, String?>): String? {
    if (imageUrls.containsKey(ImageSize.MEGA)) {
      return imageUrls[ImageSize.MEGA]
    }
    if (imageUrls.containsKey(ImageSize.EXTRALARGE)) {
      return imageUrls[ImageSize.EXTRALARGE]
    }
    if (imageUrls.containsKey(ImageSize.LARGE)) {
      return imageUrls[ImageSize.LARGE]
    }
    if (imageUrls.containsKey(ImageSize.MEDIUM)) {
      return imageUrls[ImageSize.MEDIUM]
    }
    if (imageUrls.containsKey(ImageSize.SMALL)) {
      return imageUrls[ImageSize.SMALL]
    }
    if (imageUrls.containsKey(ImageSize.UNKNOWN)) {
      return imageUrls[ImageSize.UNKNOWN]
    }
    return null
  }

  companion object {
    private const val CANDIDATE_KEY_NUMBER = 1

    const val PREFIX_EMBEDDED = "embedded://"

    const val SCHEME_EMBEDDED = "embedded"
  }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UriFetcherEntryPoint {

  fun uriFetcher(): UriFetcher
}
