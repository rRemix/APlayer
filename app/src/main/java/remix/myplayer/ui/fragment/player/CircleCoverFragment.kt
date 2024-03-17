package remix.myplayer.ui.fragment.player

import android.view.LayoutInflater
import android.view.ViewGroup
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.FragmentCoverCircleBinding
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.Command

class CircleCoverFragment : CoverFragment<FragmentCoverCircleBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCoverCircleBinding
    get() = FragmentCoverCircleBinding::inflate

  override fun playAnimation(song: Song) {
    if (!isAdded) {
      return
    }
    val operation = MusicServiceRemote.getOperation()
    val offsetX = width + binding.coverImage.width shr 1
    val startValue = 0.0
    val endValue = if (operation == Command.PREV) offsetX.toDouble() else -offsetX.toDouble()

    //封面移动动画
    outAnim = SpringSystem.create().createSpring()
    outAnim?.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        binding.coverImage.translationX = spring.currentValue.toFloat()
      }

      override fun onSpringAtRest(spring: Spring) {
//        if (cover_image.tag != requestId) {
//          cover_image.setImageURI("", null)
//        }
        binding.coverImage.translationX = startValue.toFloat()
        val endVal = 1f
        inAnim = SpringSystem.create().createSpring()
        inAnim?.addListener(object : SimpleSpringListener() {
          override fun onSpringUpdate(spring: Spring) {
            binding.coverImage.run {
              scaleX = spring.currentValue.toFloat()
              scaleY = spring.currentValue.toFloat()
            }
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