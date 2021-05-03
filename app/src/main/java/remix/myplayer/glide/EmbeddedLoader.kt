package remix.myplayer.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.glide.UriFetcher.SCHEME_EMBEDDED
import java.io.InputStream

/**
 * created by Remix on 2021/4/27
 */
class EmbeddedLoader : ModelLoader<Uri, InputStream> {
  override fun buildLoadData(model: Uri, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
    return ModelLoader.LoadData(ObjectKey(model), EmbeddedFetcher(model))
  }

  override fun handles(uri: Uri): Boolean {
    return SCHEME_EMBEDDED == uri.scheme
  }

  class Factory : ModelLoaderFactory<Uri, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, InputStream> {
      return EmbeddedLoader()
    }

    override fun teardown() {

    }

  }
}