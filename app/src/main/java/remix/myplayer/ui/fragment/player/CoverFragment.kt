package remix.myplayer.ui.fragment.player

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.ControllerListener
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.rebound.Spring
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_cover_round.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.network.RxUtil
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.util.ImageUriUtil

abstract class CoverFragment : BaseMusicFragment() {
  open val layoutId = R.layout.fragment_cover_round
  val width by lazy {
    resources.displayMetrics.widthPixels
  }
  var inAnim: Spring? = null
  var outAnim: Spring? = null
  var task: Disposable? = null
  var cover = Cover(-1, Uri.EMPTY)
  var requestId = 0
  var coverCallback: CoverCallback? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return inflater.inflate(layoutId, container, false)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    outAnim?.destroy()
    inAnim?.destroy()
  }

  //更新封面
  fun setImage(song: Song, playAnim: Boolean, updateBackground: Boolean) {
    requestId = song.id
    task?.dispose()
    task = object : ImageUriRequest<String?>() {
      override fun onError(throwable: Throwable) {
        cover = Cover(song.id, Uri.EMPTY)
        setImageUriInternal()
      }

      override fun onSuccess(result: String?) {
        cover = Cover(song.id, Uri.parse(result))
        setImageUriInternal()
      }

      override fun load(): Disposable {
        return getCoverObservable(ImageUriUtil.getSearchRequestWithAlbumType(song))
            .compose(RxUtil.applyScheduler())
            .subscribe({ result: String? -> this.onSuccess(result) }) { throwable: Throwable -> onError(throwable) }
      }
    }.load()

    if (playAnim) {
      playAnimation(song)
    }
  }

  abstract fun playAnimation(song: Song)

  private fun setImageUriInternal() {
    if (cover_image == null) {
      return
    }
    cover_image.tag = cover.id
    val controller = Fresco.newDraweeControllerBuilder()
        .setUri(cover.url)
        .setOldController(cover_image.controller)
        .setControllerListener(object : ControllerListener<ImageInfo> {
          override fun onSubmit(s: String?, o: Any?) {

          }

          override fun onFinalImageSet(s: String?, imageInfo: ImageInfo?,
                                       animatable: Animatable?) {
            if (imageInfo is CloseableBitmap) {
              coverCallback?.onBitmap(imageInfo.underlyingBitmap)
            }
          }

          override fun onIntermediateImageSet(s: String?, imageInfo: ImageInfo?) {}

          override fun onIntermediateImageFailed(s: String?, throwable: Throwable?) {
          }

          override fun onFailure(s: String?, throwable: Throwable?) {
            coverCallback?.onBitmap(null)
          }

          override fun onRelease(s: String?) {

          }
        })
        .build()
    cover_image.controller = controller
  }

  interface CoverCallback {
    fun onBitmap(bitmap: Bitmap?)
  }

  data class Cover(val id: Int, val url: Uri)
}