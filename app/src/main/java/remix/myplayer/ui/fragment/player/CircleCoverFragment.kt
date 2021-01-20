package remix.myplayer.ui.fragment.player

import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import kotlinx.android.synthetic.main.fragment_cover_circle.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.Command

class CircleCoverFragment : CoverFragment() {
  override val layoutId = R.layout.fragment_cover_circle

  override fun playAnimation(song: Song) {
    if (!isAdded) {
      return
    }
    val operation = MusicServiceRemote.getOperation()
    val offsetX = width + cover_image.width shr 1
    val startValue = 0.0
    val endValue = if (operation == Command.PREV) offsetX.toDouble() else -offsetX.toDouble()

    //封面移动动画
    outAnim = SpringSystem.create().createSpring()
    outAnim?.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        cover_image.translationX = spring.currentValue.toFloat()
      }

      override fun onSpringAtRest(spring: Spring) {
        if (cover_image.tag != requestId) {
          cover_image.setImageURI("", null)
        }
        cover_image.translationX = startValue.toFloat()
        val endVal = 1f
        inAnim = SpringSystem.create().createSpring()
        inAnim?.addListener(object : SimpleSpringListener() {
          override fun onSpringUpdate(spring: Spring) {
            if (cover_image == null) {
              return
            }
            cover_image.scaleX = spring.currentValue.toFloat()
            cover_image.scaleY = spring.currentValue.toFloat()
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