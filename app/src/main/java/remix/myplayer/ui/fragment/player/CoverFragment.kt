package remix.myplayer.ui.fragment.player

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.rebound.Spring
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.glide.GlideApp
import remix.myplayer.theme.Theme
import remix.myplayer.ui.fragment.base.BaseMusicFragment

abstract class CoverFragment<VB: ViewBinding> : BaseMusicFragment<VB>() {
  val width by lazy {
    resources.displayMetrics.widthPixels
  }
  var inAnim: Spring? = null
  var outAnim: Spring? = null
  var coverCallback: CoverCallback? = null
  var reRunnable: Runnable? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    reRunnable?.run()
    reRunnable = null
  }

  override fun onDestroyView() {
    super.onDestroyView()
    outAnim?.destroy()
    inAnim?.destroy()
  }

  fun setImage(song: Song, playAnim: Boolean, updateBackground: Boolean) {
    if (!isAdded || (context as Activity).isFinishing) {
      reRunnable = Runnable {
        setImage(song, playAnim, updateBackground)
      }
      return
    }

    GlideApp.with(this)
        .asBitmap()
        .load(song)
        .centerCrop()
        .dontAnimate()
        .placeholder(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .error(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .addListener(object : RequestListener<Bitmap> {
          override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            coverCallback?.onBitmap(null)
            return false
          }

          override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            coverCallback?.onBitmap(resource)
            return false
          }

        })
        .into(binding.root.findViewById(R.id.cover_image))

    if (playAnim) {
      playAnimation(song)
    }
  }

  fun clearAnim() {
    outAnim?.destroy()
    outAnim = null
    inAnim?.destroy()
    inAnim = null
  }

  abstract fun playAnimation(song: Song)

  interface CoverCallback {
    fun onBitmap(bitmap: Bitmap?)
  }

}