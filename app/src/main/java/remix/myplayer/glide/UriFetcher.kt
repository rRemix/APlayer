package remix.myplayer.glide

import android.net.Uri
import android.provider.MediaStore.Audio
import android.text.TextUtils
import android.util.LruCache
import remix.myplayer.App.Companion.context
import remix.myplayer.R
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.request.network.HttpClient
import remix.myplayer.util.ImageUriUtil
import remix.myplayer.util.MediaStoreUtil.getSongs
import remix.myplayer.util.SPUtil
import remix.myplayer.util.Util
import timber.log.Timber

/**
 * created by Remix on 2021/4/20
 */
object UriFetcher {
  private val BLACKLIST = listOf(
      Uri.parse("https://lastfm-img2.akamaized.net/i/u/300x300/7c58a2e3b889af6f923669cc7744c3de.png"),
      Uri.parse("https://lastfm-img2.akamaized.net/i/u/300x300/e1d60ddbcaaa6acdcbba960786f11360.png"),
      Uri.parse("http://p1.music.126.net/l8KRlRa-YLNW0GOBeN6fIA==/17914342951434926.jpg"),
      Uri.parse("http://p1.music.126.net/RCIIvR7ull5iQWN-awJ-Aw==/109951165555852156.jpg"))

  var albumVersion = 0
  var artistVersion = 0
  var playListVersion = 0

  const val TYPE_ALBUM = 10
  const val TYPE_ARTIST = 100
  const val TYPE_PLAYLIST = 1000

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

  fun updateAllVersion(){
    updateAlbumVersion()
    updateArtistVersion()
    updatePlayListVersion()
  }

  fun updateAlbumVersion(){
    albumVersion++
  }

  fun updateArtistVersion(){
    artistVersion++
  }

  fun updatePlayListVersion(){
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
      val uri = Uri.parse(cache)
      memoryCache.put(key, uri)
//      Timber.v("get from sp, uri: $uri")
      return uri
    }
    return null
  }

  private fun fetch(song: Song): Uri {
    // 自定义封面
    val customArtFile = ImageUriUtil.getCustomThumbIfExist(song.albumId, TYPE_ALBUM)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    // 内置
    if (ignoreMediaStore()) {
      val songs = getSongs(Audio.Media._ID + "=" + song.id, null)
      if (songs.isNotEmpty()) {
        return Uri.parse(PREFIX_EMBEDDED + songs[0].data)
      }
    } else if (ImageUriUtil.isAlbumThumbExistInMediaCache(song.artUri)) {
      return song.artUri
    }

    // 网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMAlbum = HttpClient.searchLastFMAlbum(song.album, song.artist, null).blockingGet()
          val lastFMUri = ImageUriUtil.getLargestAlbumImageUrl(lastFMAlbum.album?.image)
          if (!TextUtils.isEmpty(lastFMUri)) {
            return Uri.parse(lastFMUri)
          }
        } else {
          val neteaseResponse = HttpClient.searchNeteaseSong(ImageUriUtil.getNeteaseSearchKey(song), 0, 1).blockingGet()
          val song_id = neteaseResponse?.result?.songs?.get(0)?.id
          val neteaseDetailResponse = HttpClient.searchNeteaseDetail(song_id.toString().toInt(), "[$song_id]").blockingGet()
          val neteaseUri = neteaseDetailResponse?.songs?.get(0)?.album?.picUrl
          //val neteaseUri = neteaseResponse?.result?.songs?.get(0)?.album?.picUrl
          if (!TextUtils.isEmpty(neteaseUri)) {
            return Uri.parse(neteaseUri)
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
    val customArtFile = ImageUriUtil.getCustomThumbIfExist(album.albumID, TYPE_ALBUM)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    // 内置
    if (ignoreMediaStore()) {
      val songs = getSongs(Audio.Media.ALBUM_ID + "=" + album.albumID, null)
      if (songs.isNotEmpty()) {
        return Uri.parse(PREFIX_EMBEDDED + songs[0].data)
      }
    } else if (ImageUriUtil.isAlbumThumbExistInMediaCache(album.artUri)) {
      return album.artUri
    }

    // 网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMAlbum = HttpClient.searchLastFMAlbum(album.album, album.artist, null).blockingGet()
          val lastFMUri = ImageUriUtil.getLargestAlbumImageUrl(lastFMAlbum.album?.image)
          if (!TextUtils.isEmpty(lastFMUri)) {
            return Uri.parse(lastFMUri)
          }
        } else {
          val neteaseResponse = HttpClient.searchNeteaseAlbum(ImageUriUtil.getNeteaseSearchKey(album), 0, 1).blockingGet()
          val neteaseUri = neteaseResponse?.result?.albums?.get(0)?.picUrl
          if (!TextUtils.isEmpty(neteaseUri)) {
            return Uri.parse(neteaseUri)
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
    val customArtFile = ImageUriUtil.getCustomThumbIfExist(artist.artistID, TYPE_ARTIST)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    // 内置
    val imageUrl = ImageUriUtil.getArtistArt(artist.artistID)
    if (imageUrl.isNotEmpty()) {
      return Uri.parse(imageUrl)
    }

    //网络
    if (canDownloadCover()) {
      try {
        if (downloadFromLastFM()) {
          val lastFMArtist = HttpClient.searchLastFMArtist(artist.artist, null).blockingGet()
          val lastFMUri = ImageUriUtil.getLargestArtistImageUrl(lastFMArtist.artist?.image)
          if (!TextUtils.isEmpty(lastFMUri)) {
            return Uri.parse(lastFMUri)
          }
        } else {
          val neteaseResponse = HttpClient.searchNeteaseArtist(ImageUriUtil.getNeteaseSearchKey(artist), 0, 1).blockingGet()
          //      imageUrl = response.getResult().getArtists().get(0).getPicUrl();
          val neteaseUri = neteaseResponse?.result?.artists?.get(0)?.picUrl
          if (!TextUtils.isEmpty(neteaseUri)) {
            return Uri.parse(neteaseUri)
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
    val customArtFile = ImageUriUtil.getCustomThumbIfExist(playList.id, TYPE_PLAYLIST)
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

  private fun ignoreMediaStore(): Boolean {
    return SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE, false)
  }

  private fun downloadFromLastFM(): Boolean {
    return SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, DOWNLOAD_LASTFM) == DOWNLOAD_LASTFM
  }

  private fun canDownloadCover(): Boolean {
    val current = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER, context.getString(R.string.always))
    return context.getString(R.string.always) == current || (context.getString(R.string.wifi_only) == current && Util.isWifi(context))
  }

}