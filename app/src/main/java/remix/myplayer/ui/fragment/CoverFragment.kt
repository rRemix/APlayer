package remix.myplayer.ui.fragment

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
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_cover.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote.getOperation
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.Command
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.util.ImageUriUtil

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 专辑封面Fragment
 */
class CoverFragment : BaseMusicFragment() {
  var coverCallback: CoverCallback? = null
  private var task: Disposable? = null
  private var width = 0
  private var cover = Cover(-1, Uri.EMPTY)
  private var requestId = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = CoverFragment::class.java.simpleName
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    width = resources.displayMetrics.widthPixels
    return inflater.inflate(R.layout.fragment_cover, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
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

  /**
   * 操作为上一首歌曲时，显示往左侧消失的动画 下一首歌曲时，显示往右侧消失的动画
   */
  private fun playAnimation(song: Song) {
    if (!isAdded) {
      return
    }
    val operation = getOperation()
    val offsetX = width + cover_image.width shr 1
    val startValue = 0.0
    val endValue = if (operation == Command.PREV) offsetX.toDouble() else -offsetX.toDouble()

    //封面移动动画
    val outAnim = SpringSystem.create().createSpring()
    outAnim.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        cover_container.translationX = spring.currentValue.toFloat()
      }

      override fun onSpringAtRest(spring: Spring) {
        if (cover_image.tag != requestId) {
          cover_image.setImageURI("", null)
        }
        cover_container.translationX = startValue.toFloat()
        val endVal = 1f
        val inAnim = SpringSystem.create().createSpring()
        inAnim.addListener(object : SimpleSpringListener() {
          override fun onSpringUpdate(spring: Spring) {
            if (cover_image == null) {
              return
            }
            cover_container.scaleX = spring.currentValue.toFloat()
            cover_container.scaleY = spring.currentValue.toFloat()
          }

          override fun onSpringActivate(spring: Spring) {}
        })
        inAnim.currentValue = 0.85
        inAnim.endValue = endVal.toDouble()
      }
    })
    outAnim.isOvershootClampingEnabled = true
    outAnim.currentValue = startValue
    outAnim.endValue = endValue
  }

  private fun setImageUriInternal() {
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