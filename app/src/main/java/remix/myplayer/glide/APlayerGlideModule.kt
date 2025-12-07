package remix.myplayer.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import remix.myplayer.data.model.audio.APlayerModel
import java.io.InputStream

@GlideModule
class APlayerGlideModule : AppGlideModule() {

  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    registry.append(Uri::class.java, InputStream::class.java, EmbeddedLoader.Factory())
    registry.append(
      APlayerModel::class.java,
      InputStream::class.java,
      APlayerUriLoader.Factory(context)
    )
  }

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    super.applyOptions(context, builder)
    builder.setLogLevel(Log.ERROR)
    builder.setDiskCacheExecutor(
      GlideExecutor.newSourceBuilder()
        .setName("custom-disk-cache")
        .setThreadCount(1)
        .setThreadTimeoutMillis(10000)
        .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.LOG)
        .build()
    )
  }
}

fun RequestBuilder<Drawable>.addBitmapListener(
  onBitmap: (Bitmap?) -> Unit
): RequestBuilder<Drawable> {
  return addListener(object : RequestListener<Drawable> {
    override fun onLoadFailed(
      e: GlideException?,
      model: Any?,
      target: Target<Drawable?>,
      isFirstResource: Boolean
    ): Boolean {
      onBitmap(null)
      return false
    }

    override fun onResourceReady(
      resource: Drawable,
      model: Any,
      target: Target<Drawable?>?,
      dataSource: DataSource,
      isFirstResource: Boolean
    ): Boolean {
      val bmp = if (resource is BitmapDrawable) resource.bitmap else null
      onBitmap(bmp)
      return false
    }
  })
}