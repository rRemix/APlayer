package remix.myplayer.service

import android.os.CountDownTimer
import android.os.Handler
import androidx.annotation.FloatRange
import timber.log.Timber

/**
 * Created by Remix on 2018/3/13.
 */

class VolumeController(private val service: MusicService) {
  private val handler: Handler = Handler()
  private val fadeInRunnable: Runnable = Runnable {
    val mediaPlayer = service.mediaPlayer
    object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
      override fun onFinish() {
        directTo(1f)
      }

      override fun onTick(millisUntilFinished: Long) {
        val volume = 1f - millisUntilFinished * 1.0f / DURATION_IN_MS
        try {
          mediaPlayer.setVolume(volume, volume)
        } catch (e: IllegalStateException) {
          Timber.v(e)
        }
      }
    }.start()
  }
  private val fadeOutRunnable: Runnable = Runnable {
    val mediaPlayer = service.mediaPlayer
    object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
      override fun onTick(millisUntilFinished: Long) {
        val volume = millisUntilFinished * 1.0f / DURATION_IN_MS
        try {
          mediaPlayer.setVolume(volume, volume)
        } catch (e: IllegalStateException) {
          Timber.v(e)
        }
      }

      override fun onFinish() {
        directTo(0f)
        try {
          mediaPlayer.pause()
        } catch (e: IllegalStateException) {
          Timber.v(e)
        }
      }

    }.start()
  }

  fun directTo(@FloatRange(from = 0.0, to = 1.0) toVolume: Float) {
    directTo(toVolume, toVolume)
  }

  private fun directTo(@FloatRange(from = 0.0, to = 1.0) leftVolume: Float, @FloatRange(from = 0.0, to = 1.0) rightVolume: Float) {
    val mediaPlayer = service.mediaPlayer
    try {
      mediaPlayer.setVolume(leftVolume, rightVolume)
    } catch (e: IllegalStateException) {
      Timber.v(e)
    }
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