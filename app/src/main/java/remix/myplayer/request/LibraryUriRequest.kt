package remix.myplayer.request

import android.graphics.drawable.Animatable
import android.net.Uri
import android.text.TextUtils
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import remix.myplayer.request.network.RxUtil
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by Remix on 2017/12/4.
 */

open class LibraryUriRequest(image: SimpleDraweeView, protected val mRequest: UriRequest,
                             config: RequestConfig) : ImageUriRequest<String>(config) {

  val mImageRef = WeakReference(image)

  override fun onError(throwable: Throwable?) {
//    mImageRef.get()?.setImageURI(Uri.EMPTY)
    Timber.v("onError() %s", throwable?.message)
  }

  open fun onSave(result: String) {

  }

  override fun onSuccess(result: String?) {
    if (TextUtils.isEmpty(result) || result == null) {
      onError(Throwable(ERROR_NO_RESULT))
      return
    }
    if (ImageUriRequest.BLACKLIST.contains(result)) {
      onError(Throwable(ERROR_BLACKLIST))
      return
    }
    onSave(result)
    Timber.v("onSuccess() %s", result)
    val imageRequestBuilder = ImageRequestBuilder
        .newBuilderWithSource(Uri.parse(result))
    if (mConfig.isResize) {
      imageRequestBuilder.resizeOptions = ResizeOptions.forDimensions(mConfig.width, mConfig.height)
    }

    val image = mImageRef.get() ?: return
    val controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(imageRequestBuilder.build())
        .setOldController(image.controller)
        .setControllerListener(object : ControllerListener<ImageInfo> {
          override fun onSubmit(s: String?, o: Any?) {

          }

          override fun onFinalImageSet(s: String?, imageInfo: ImageInfo?,
                                       animatable: Animatable?) {
          }

          override fun onIntermediateImageSet(s: String?, imageInfo: ImageInfo?) {}

          override fun onIntermediateImageFailed(s: String?, throwable: Throwable?) {
            Timber.v("onIntermediateImageFailed() %s", throwable.toString())
          }

          override fun onFailure(s: String?, throwable: Throwable?) {
            Timber.v("onFailure %s", throwable.toString())
          }

          override fun onRelease(s: String?) {

          }
        })
        .build()

    image.controller = controller
  }

  override fun load(): Disposable {
    return getCoverObservable(mRequest)
        .compose(RxUtil.applyScheduler())
        .subscribeWith(object : DisposableObserver<String>() {
          override fun onStart() {
            mImageRef.get()?.setImageURI(Uri.EMPTY)
          }

          override fun onNext(s: String) {
            onSuccess(s)
          }

          override fun onError(e: Throwable) {
            this@LibraryUriRequest.onError(e)
          }

          override fun onComplete() {

          }
        })

  }

  companion object {
    private val TAG = LibraryUriRequest::class.java.simpleName
    const val ERROR_NO_RESULT = "empty result"
    const val ERROR_BLACKLIST = "blacklist"
  }
}
