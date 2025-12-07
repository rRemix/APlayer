package remix.myplayer.glide

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import dagger.hilt.android.EntryPointAccessors
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.CoverPrefsEntryPoint
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

class APlayerUriLoader(
  private val concreteLoader: ModelLoader<Uri, InputStream>,
  private val coverPrefs: CoverPrefs,
  private val uriFetcher: UriFetcher
) : ModelLoader<APlayerModel, InputStream> {

  override fun buildLoadData(
    model: APlayerModel,
    width: Int,
    height: Int,
    options: Options
  ): ModelLoader.LoadData<InputStream> {
    val version = when (model) {
      is Song, is Album -> coverPrefs.getAlbumVersion()
      is Artist -> coverPrefs.getArtistVersion()
      is PlayList -> coverPrefs.getPlayListVersion()
      else -> 0
    }
    val key = uriFetcher.cacheKey(model)
    return ModelLoader.LoadData(
      APlayerSignature(key, version),
      APlayerFetcher(model, concreteLoader, width, height, options, uriFetcher)
    )
  }

  override fun handles(model: APlayerModel): Boolean {
    return model is Song || model is Album || model is Artist || model is PlayList || model is Genre
  }

  class Factory(private val context: Context) : ModelLoaderFactory<APlayerModel, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<APlayerModel, InputStream> {
      val coverPrefs =
        EntryPointAccessors.fromApplication(context, CoverPrefsEntryPoint::class.java).coverPrefs()

      val uriFetcher =
        EntryPointAccessors.fromApplication(context, UriFetcherEntryPoint::class.java).uriFetcher()

      return APlayerUriLoader(
        multiFactory.build(Uri::class.java, InputStream::class.java),
        coverPrefs,
        uriFetcher
      )
    }

    override fun teardown() {
    }
  }

  private class APlayerFetcher(
    private val model: APlayerModel,
    private val concreteLoader: ModelLoader<Uri, InputStream>,
    private val width: Int,
    private val height: Int,
    private val options: Options,
    private val uriFetcher: UriFetcher
  ) : DataFetcher<InputStream> {

    @Volatile
    private var delegateFetcher: DataFetcher<InputStream>? = null

    @Volatile
    private var resolvedDataSource: DataSource? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
      try {
        val uri = uriFetcher.fetch(model)
        if (uri == Uri.EMPTY) {
          callback.onLoadFailed(Exception("Empty URI for model: $model"))
          return
        }

        val loadData = concreteLoader.buildLoadData(uri, width, height, options)
        if (loadData == null) {
          callback.onLoadFailed(Exception("Failed to build load data for uri: $uri"))
          return
        }

        delegateFetcher = loadData.fetcher
        resolvedDataSource = delegateFetcher?.dataSource
        delegateFetcher?.loadData(priority, callback)
      } catch (e: Exception) {
        callback.onLoadFailed(e)
      }
    }

    override fun cleanup() {
      delegateFetcher?.cleanup()
    }

    override fun cancel() {
      delegateFetcher?.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = resolvedDataSource ?: delegateFetcher?.dataSource ?: DataSource.REMOTE
  }
}

private class APlayerSignature(
  private val key: String,
  private val version: Int
) : Key {
  override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    messageDigest.update(key.toByteArray(Key.CHARSET))
    messageDigest.update(ByteBuffer.allocate(4).putInt(version).array())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is APlayerSignature) return false

    if (key != other.key) return false
    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    var result = key.hashCode()
    result = 31 * result + version
    return result
  }
}
