package remix.myplayer.ui.fragment.player

import android.os.Bundle
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import kotlinx.android.synthetic.main.fragment_cover_round.*
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote.getOperation
import remix.myplayer.service.Command

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 专辑封面Fragment
 */
class RoundCoverFragment : CoverFragment() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = RoundCoverFragment::class.java.simpleName
  }

  /**
   * 操作为上一首歌曲时，显示往左侧消失的动画 下一首歌曲时，显示往右侧消失的动画
   */
  override fun playAnimation(song: Song) {
    if (!isAdded) {
      return
    }
    val operation = getOperation()
    val offsetX = width + cover_image.width shr 1
    val startValue = 0.0
    val endValue = if (operation == Command.PREV) offsetX.toDouble() else -offsetX.toDouble()

    //封面移动动画
    outAnim = SpringSystem.create().createSpring()
    outAnim?.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        cover_container.translationX = spring.currentValue.toFloat()
      }

      override fun onSpringAtRest(spring: Spring) {
        if (cover_image.tag != requestId) {
          cover_image.setImageURI("", null)
        }
        cover_container.translationX = startValue.toFloat()
        val endVal = 1f
        inAnim = SpringSystem.create().createSpring()
        inAnim?.addListener(object : SimpleSpringListener() {
          override fun onSpringUpdate(spring: Spring) {
            if (cover_image == null) {
              return
            }
            cover_container.scaleX = spring.currentValue.toFloat()
            cover_container.scaleY = spring.currentValue.toFloat()
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