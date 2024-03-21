package remix.myplayer.ui.fragment.player

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.FragmentCoverRoundBinding
import remix.myplayer.helper.MusicServiceRemote.getOperation
import remix.myplayer.service.Command

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 专辑封面Fragment
 */
class RoundCoverFragment : CoverFragment<FragmentCoverRoundBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCoverRoundBinding
    get() = FragmentCoverRoundBinding::inflate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = RoundCoverFragment::class.java.simpleName
  }

  /**
   * 操作为上一首歌曲时，显示往左侧消失的动画 下一首歌曲时，显示往右侧消失的动画
   */
  override fun playAnimation(song: Song) {
    val coverContainer = binding.coverContainer
    if (!isAdded || (context as Activity).isFinishing || coverContainer == null) {
      return
    }
    val operation = getOperation()
    val offsetX = width + binding.coverImage.width shr 1
    val startValue = 0.0
    val endValue = if (operation == Command.PREV) offsetX.toDouble() else -offsetX.toDouble()

    //封面移动动画
    outAnim = SpringSystem.create().createSpring()
    outAnim?.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        coverContainer.translationX = spring.currentValue.toFloat()
      }

      override fun onSpringAtRest(spring: Spring) {
//        if (cover_image.tag != requestId) {
//          cover_image.setImageURI("", null)
//        }
        coverContainer.translationX = startValue.toFloat()
        val endVal = 1f
        inAnim = SpringSystem.create().createSpring()
        inAnim?.addListener(object : SimpleSpringListener() {
          override fun onSpringUpdate(spring: Spring) {
            coverContainer.scaleX = spring.currentValue.toFloat()
            coverContainer.scaleY = spring.currentValue.toFloat()
          }

          override fun onSpringActivate(spring: Spring) {}
        })
        inAnim?.currentValue = 0.85
        inAnim?.endValue = endVal.toDouble()
      }
    })
    outAnim?.isOvershootClampingEnabled = true
    outAnim?.currentValue = startValue
    outAnim?.endValue = endValue
  }
}