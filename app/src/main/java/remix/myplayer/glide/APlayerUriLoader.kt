package remix.myplayer.glide

import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.data.bean.mp3.APlayerModel
import remix.myplayer.data.bean.mp3.Album
import remix.myplayer.data.bean.mp3.Artist
import remix.myplayer.data.bean.mp3.Genre
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.data.db.room.entity.PlayList
import java.io.InputStream

class APlayerUriLoader(private val concreteLoader: ModelLoader<Uri, InputStream>) :
  ModelLoader<APlayerModel, InputStream> {

  override fun buildLoadData(
    model: APlayerModel,
    width: Int,
    height: Int,
    options: Options
  ): ModelLoader.LoadData<InputStream> {
    return ModelLoader.LoadData(
      ObjectKey(model),
      APlayerFetcher(model, concreteLoader, width, height, options)
    )
  }

  override fun handles(model: APlayerModel): Boolean {
    return model is Song || model is Album || model is Artist || model is PlayList || model is Genre
  }

  class Factory : ModelLoaderFactory<APlayerModel, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<APlayerModel, InputStream> {
      return APlayerUriLoader(multiFactory.build(Uri::class.java, InputStream::class.java))
    }

    override fun teardown() {
    }
  }

  private class APlayerFetcher(
    private val model: APlayerModel,
    private val concreteLoader: ModelLoader<Uri, InputStream>,
    private val width: Int,
    private val height: Int,
    private val options: Options
  ) : DataFetcher<InputStream> {

    @Volatile
    private var delegateFetcher: DataFetcher<InputStream>? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
      try {
        val uri = UriFetcher.fetch(model)
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

    override fun getDataSource(): DataSource = DataSource.REMOTE
  }
}