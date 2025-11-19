package remix.myplayer.service

import android.os.CountDownTimer
import android.os.Handler
import androidx.annotation.FloatRange

/**
 * Created by Remix on 2018/3/13.
 */

class VolumeController(private val service: MusicService) {

  private val handler: Handler = Handler()
  private val fadeInRunnable: Runnable = Runnable {
    object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
      override fun onFinish() {
        directTo(1f)
      }

      override fun onTick(millisUntilFinished: Long) {
        val volume = 1f - millisUntilFinished * 1.0f / DURATION_IN_MS
        service.playback.setVolume(volume)
      }
    }.start()
  }
  private val fadeOutRunnable: Runnable = Runnable {
    object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
      override fun onTick(millisUntilFinished: Long) {
        val volume = millisUntilFinished * 1.0f / DURATION_IN_MS
        service.playback.setVolume(volume)
      }

      override fun onFinish() {
//        service.playback.setVolume(0f)
        service.playback.pause()
      }

    }.start()
  }

  fun directTo(@FloatRange(from = 0.0, to = 1.0) toVolume: Float) {
    directTo(toVolume, toVolume)
  }

  private fun directTo(
    @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
    @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
  ) {
    service.playback.setVolume(leftVolume)
  }

  /**
   * 淡入
   */
  fun fadeIn() {
    handler.removeCallbacks(fadeInRunnable)
    handler.removeCallbacks(fadeOutRunnable)
    handler.post(fadeInRunnable)
  }

  /**
   * 淡出
   */
  fun fadeOut() {
    handler.removeCallbacks(fadeInRunnable)
    handler.removeCallbacks(fadeOutRunnable)
    handler.post(fadeOutRunnable)
  }

  companion object {

    private const val DURATION_IN_MS = 600L
  }
}