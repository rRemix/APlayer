package remix.myplayer.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.*
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.bumptech.glide.module.AppGlideModule
import remix.myplayer.App
import remix.myplayer.bean.mp3.ImageUri
import remix.myplayer.util.SPUtil
import java.io.InputStream

@GlideModule
class APlayerGlideModule : AppGlideModule() {

  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    registry.append(ImageUri::class.java, InputStream::class.java, object : ModelLoaderFactory<ImageUri, InputStream> {
      override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ImageUri, InputStream> {
        return ImageUriLoader(multiFactory.build(GlideUrl::class.java, InputStream::class.java), ModelCache())
      }

      override fun teardown() {
      }
    })
  }

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    super.applyOptions(context, builder)
  }

  class ImageUriLoader(
      concreteLoader: ModelLoader<GlideUrl, InputStream>,
      modelCache: ModelCache<ImageUri, GlideUrl>) : BaseGlideUrlLoader<ImageUri>(concreteLoader, modelCache) {
    override fun getUrl(imageUri: ImageUri, width: Int, height: Int, options: Options): String {
      return imageUri.getImageUri().toString()
    }

    override fun handles(imageUri: ImageUri): Boolean {
      return !SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE,false)
    }
  }
}