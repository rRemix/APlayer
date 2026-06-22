package remix.myplayer.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import androidx.annotation.FloatRange

/**
 * Created by Remix on 2018/3/13.
 */

class VolumeController(private val service: MusicService) {

  private val handler = Handler(Looper.getMainLooper())
  private var animator: ValueAnimator? = null
  private var lastVolume = 1f

  fun directTo(@FloatRange(from = 0.0, to = 1.0) toVolume: Float) {
    directTo(toVolume, toVolume)
  }

  private fun directTo(
    @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
    @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
  ) {
    val volume = leftVolume.coerceIn(0f, 1f)
    cancelAnimator()
    service.playback.setVolume(volume)
    lastVolume = volume
  }

  /**
   * 淡入
   */
  fun fadeIn() {
    fadeTo(1f)
  }

  /**
   * 淡出
   */
  fun fadeOut() {
    fadeTo(0f, onEnd = { service.playback.pause() })
  }

  private fun fadeTo(
    @FloatRange(from = 0.0, to = 1.0) targetVolume: Float,
    onEnd: (() -> Unit)? = null
  ) {
    val target = targetVolume.coerceIn(0f, 1f)
    handler.post {
      cancelAnimator()
      animator = ValueAnimator.ofFloat(lastVolume, target).apply {
        duration = DURATION_IN_MS
        addUpdateListener {
          val volume = it.animatedValue as Float
          service.playback.setVolume(volume)
          lastVolume = volume
        }
        addListener(object : AnimatorListenerAdapter() {
          private var canceled = false

          override fun onAnimationCancel(animation: Animator) {
            canceled = true
          }

          override fun onAnimationEnd(animation: Animator) {
            if (animator === animation) {
              animator = null
            }
            if (!canceled) {
              onEnd?.invoke()
            }
          }
        })
      }
      animator?.start()
    }
  }

  private fun cancelAnimator() {
    animator?.cancel()
    animator = null
  }

  companion object {

    private const val DURATION_IN_MS = 400L
  }
}
