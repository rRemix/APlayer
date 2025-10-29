package remix.myplayer.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import remix.myplayer.data.bean.mp3.APlayerModel
import remix.myplayer.data.bean.mp3.Album
import remix.myplayer.data.bean.mp3.Artist
import remix.myplayer.data.bean.mp3.Genre
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.data.db.room.entity.PlayList
import java.io.InputStream

class APlayerUriLoader(private val concreteLoader: ModelLoader<Uri, InputStream>) : ModelLoader<APlayerModel, InputStream> {
  private val uriFetcher = UriFetcher

  override fun buildLoadData(model: APlayerModel, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
    val uri = uriFetcher.fetch(model)
    return concreteLoader.buildLoadData(uri, width, height, options)
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
}