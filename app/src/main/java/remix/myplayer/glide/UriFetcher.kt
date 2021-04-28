package remix.myplayer.glide

import android.net.Uri
import android.text.TextUtils
import android.util.LruCache
import com.facebook.common.util.UriUtil
import remix.myplayer.App
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.PlayList
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.ImageUriRequest.DOWNLOAD_LASTFM
import remix.myplayer.request.network.HttpClient
import remix.myplayer.util.ImageUriUtil
import remix.myplayer.util.SPUtil
import timber.log.Timber
import java.lang.RuntimeException

/**
 * created by Remix on 2021/4/20
 */
class UriFetcher {

  private val cache: LruCache<Int, Uri> = LruCache(200)

  fun fetch(model: Any): Uri {
    return when (model) {
      is Song -> {
        fetchActual(model)
      }
      is Album -> {
        fetchActual(model)
      }
      is Artist -> {
        fetchActual(model)
      }
      is PlayList -> {
        fetchActual(model)
      }
      else -> {
        throw IllegalArgumentException("unknown model: " + { model::class.java.simpleName })
      }
    }
  }

  fun fetchActual(song: Song): Uri {
    if (cache.get(song.hashCode()) != null) {
      return cache.get(song.hashCode())
    }

    // 自定义封面
    val customArtFile = ImageUriUtil.getCustomThumbIfExist(song.albumId, ImageUriRequest.URL_ALBUM)
    if (customArtFile != null && customArtFile.exists()) {
      return Uri.fromFile(customArtFile)
    }

    // 内置
    if (IGNORE_MEDIA_STORE) {
      return Uri.parse(PREFIX_EMBEDDED + song.data)
    } else if (ImageUriUtil.isAlbumThumbExistInMediaCache(song.artUri)) {
      return song.artUri
    }

    // 网络
    try {
      if (DOWNLOAD_SOURCE == DOWNLOAD_LASTFM) {
        val lastFmAlbum = HttpClient.getInstance().searchLastFMAlbum(song.album, song.artist, null).blockingGet()
        val lastFMUri = ImageUriUtil.getLargestAlbumImageUrl(lastFmAlbum.album?.image)
        if (!TextUtils.isEmpty(lastFMUri) && UriUtil.isNetworkUri(Uri.parse(lastFMUri))) {
          return Uri.parse(lastFMUri)
        }
      } else {
        val neteaseResponse = HttpClient.getInstance().searchNeteaseSong(ImageUriUtil.getNeteaseSearchKey(song), 0, 1).blockingGet()
        val neteaseUri = neteaseResponse?.result?.songs?.get(0)?.album?.picUrl
        if (!TextUtils.isEmpty(neteaseUri)) {
          return Uri.parse(neteaseUri)
        }
      }
    } catch (e: Exception) {
      Timber.v(e)
    }

    return Uri.EMPTY
  }

  fun fetchActual(album: Album): Uri {

    return Uri.EMPTY
  }

  fun fetchActual(artist: Artist): Uri {
    return Uri.EMPTY
  }

  fun fetchActual(playList: PlayList): Uri {
    return Uri.EMPTY
  }

  companion object {
    @Volatile
    private var INSTANCE: UriFetcher? = null

    @JvmStatic
    fun getInstance(): UriFetcher = INSTANCE ?: synchronized(UriFetcher::class.java) {
      INSTANCE ?: UriFetcher().also { INSTANCE = it }
    }

    var IGNORE_MEDIA_STORE = SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE, false)

    var DOWNLOAD_SOURCE = SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, DOWNLOAD_LASTFM)

    const val PREFIX_EMBEDDED = "embedded://"

    const val SCHEME_EMBEDDED = "embedded"
  }
}