package remix.myplayer.glide

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore.Audio
import android.util.LruCache
import androidx.core.net.toUri
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import remix.myplayer.App.Companion.context
import remix.myplayer.R
import remix.myplayer.bean.lastfm.Image
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.request.netease.NetEaseClientEntryPoint
import remix.myplayer.request.network.LastFMApi
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MediaStoreUtil.getSongs
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SearchKeyUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File

/**
 * created by Remix on 2021/4/20
 */
object UriFetcher {

  private val BLACKLIST = listOf(
    Uri.parse("https://lastfm-img2.akamaized.net/i/u/300x300/7c58a2e3b889af6f923669cc7744c3de.png"),
    Uri.parse("https://lastfm-img2.akamaized.net/i/u/300x300/e1d60ddbcaaa6acdcbba960786f11360.png"),
    Uri.parse("http://p1.music.126.net/l8KRlRa-YLNW0GOBeN6fIA==/17914342951434926.jpg"),
    Uri.parse("http://p1.music.126.net/RCIIvR7ull5iQWN-awJ-Aw==/109951165555852156.jpg")
  )

  private val neClient = EntryPointAccessors.fromApplication(
    context.applicationContext,
    NetEaseClientEntryPoint::class.java
  ).netEaseClient()

  private val lastFMApi = EntryPointAccessors.fromApplication(
    context.applicationContext,
    LastFMApi.LastFMApiEntryPoint::class.java
  ).lastFMApi()

  var albumVersion = 0
  var artistVersion = 0
  var playListVersion = 0

//  const val TYPE_ALBUM = 10
//  const val TYPE_ARTIST = 100
//  const val TYPE_PLAYLIST = 1000

  const val DOWNLOAD_LASTFM = 0
  const val DOWNLOAD_NETEASE = 1

  const val PREFIX_EMBEDDED = "embedded://"

  const val SCHEME_EMBEDDED = "embedded"

  private val memoryCache: LruCache<Int, Uri> = LruCache(200)

  fun fetch(model: Any): Uri {
    val key = model.hashCode()

    val fromCache = getFromCache(key)
    if (fromCache != null) {
      Timber.v("from cache: $fromCache")
      return fromCache
    }

    val uri = when (model) {
      is Song -> {
        fetch(model)
      }

      is Album -> {
        fetch(model)
      }

      is Artist -> {
        fetch(model)
      }

      is PlayList -> {
        fetch(model)
      }

      is Genre -> {
        fetch(model)
      }

      else -> {
        throw IllegalArgumentException("unknown model: " + { model::class.java.simpleName })
      }
    }

    if (BLACKLIST.contains(uri) || uri == Uri.EMPTY) {
      return Uri.EMPTY
    }

    Timber.v("uri: $uri")
    memoryCache.put(key, uri)
    SPUtil.putValue(context, SPUtil.COVER_KEY.NAME, key.toString(), uri.toString())

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
    memoryCache.evictAll()
    SPUtil.deleteFile(context, SPUtil.COVER_KEY.NAME)
  }

//  fun clearCache(model: APlayerModel) {
//    memoryCache.remove(model.hashCode())
//    SPUtil.putValue(context, SPUtil.COVER_KEY.NAME, model.hashCode().toString(), "")
//  }

  private fun getFromCache(key: Int): Uri? {
    val uri: Uri? = getFromMemory(key)
    if (uri != null) {
      return uri
    }

    return getFromSP(key)
  }

  private fun getFromMemory(key: Int): Uri? {
    val cache = memoryCache.get(key)
    if (cache != null) {
//      Timber.v("get from memory, uri: $cache")
    }
    return cache
  }

  private fun getFromSP(key: Int): Uri? {
    val cache = SPUtil.getValue(context, SPUtil.COVER_KEY.NAME, key, "")
    if (cache.isNotEmpty()) {
      val uri = cache.toUri()
      memoryCache.put(key, uri)
//      Timber.v("get from sp, uri: $uri")
      return uri
    }
    return null
  }

  private fun fetch(song: Song): Uri {
    if (song.isLocal()) { // 仅本地歌曲
      if (song.albumId <= 0 || song.id <= 0) {
        return Uri.EMPTY
      }
      // 自定义封面
      val customArtFile = getCustomThumbIfExist(song.albumId, Constants.ALBUM)
      if (customArtFile != null && customArtFile.exists()) {
        return Uri.fromFile(customArtFile)
      }

      // 内置
      if (ignoreMediaStore()) {
        val songs = getSongs(Audio.Media._ID + "=" + song.id, null)
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
          val lastFMUri = getLargestAlbumImageUrl(lastFMAlbum.album?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val neSong = neClient.searchSong(SearchKeyUtil.getNetEaseSearchKey(song))
          if (neSong?.al?.picUrl?.isNotEmpty() == true) {
            return neSong.al.picUrl.toUri()
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
    val customArtFile = getCustomThumbIfExist(album.albumID, Constants.ALBUM)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    // 内置
    if (ignoreMediaStore()) {
      val songs = getSongs(Audio.Media.ALBUM_ID + "=" + album.albumID, null)
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
          val lastFMUri = getLargestAlbumImageUrl(lastFMAlbum.album?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val neAlbum = neClient.searchAlbum(SearchKeyUtil.getNetEaseSearchKey(album))
          if (neAlbum != null && !neAlbum.picUrl.isNullOrEmpty()) {
            return neAlbum.picUrl.toUri()
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
    val customArtFile = getCustomThumbIfExist(artist.artistID, Constants.ARTIST)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

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
          val lastFMUri = getLargestArtistImageUrl(lastFMArtist.artist?.image)
          if (!lastFMUri.isNullOrEmpty()) {
            return lastFMUri.toUri()
          }
        } else {
          val neArtist = neClient.searchArtist(SearchKeyUtil.getNetEaseSearchKey(artist))
          if (neArtist != null && !neArtist.picUrl.isNullOrEmpty()) {
            return neArtist.picUrl.toUri()
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
    val customArtFile = getCustomThumbIfExist(playList.id, Constants.PLAYLIST)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    val songs = DatabaseRepository.getInstance()
      .getPlayList(playList.id)
      .flatMap {
        DatabaseRepository.getInstance()
          .getPlayListSongs(context, it, true)
      }
      .blockingGet()

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
    val songs = MediaStoreUtil.getSongsByGenreId(genre.id)

    var uri: Uri
    for (song in songs) {
      uri = fetch(song)
      if (uri != Uri.EMPTY) {
        return uri
      }
    }

    return Uri.EMPTY
  }

  private fun ignoreMediaStore(): Boolean {
    return SPUtil.getValue(
      context,
      SPUtil.SETTING_KEY.NAME,
      SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE,
      false
    )
  }

  private fun downloadFromLastFM(): Boolean {
    return SPUtil.getValue(
      context,
      SPUtil.SETTING_KEY.NAME,
      SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE,
      DOWNLOAD_LASTFM
    ) == DOWNLOAD_LASTFM
  }

  private fun canDownloadCover(): Boolean {
    val current = SPUtil.getValue(
      context,
      SPUtil.SETTING_KEY.NAME,
      SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
      context.getString(R.string.always)
    )
    return context.getString(R.string.always) == current || (context.getString(R.string.wifi_only) == current && Util.isWifi(
      context
    ))
  }

  /**
   * 根据artistId搜索MediaStore中是否存在封面
   */
  fun getArtistArt(artistId: Long): String {
    val songs = getSongs(Audio.Media.ARTIST_ID + " = " + artistId, null)
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
  fun isAlbumThumbExistInMediaCache(uri: Uri): Boolean {
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
  fun getCustomThumbIfExist(id: Long, type: Int): File? {
    val img = File(DiskCache.getDiskCacheDir(context, "thumbnail"), "$type-$id.jpg")
    if (img.exists()) {
      return img
    }
    return null
  }

  private enum class ImageSize {
    SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
  }

  /**
   * 解析LastFm返回的最大封面
   */
  private fun getLargestAlbumImageUrl(images: List<Image>?): String? {
    val imageUrls = HashMap<ImageSize, String?>()
    if (images == null || images.isEmpty()) {
      return ""
    }
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

  /**
   * 解析LastFm返回的最大封面
   */
  fun getLargestArtistImageUrl(images: List<Image>?): String? {
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
}