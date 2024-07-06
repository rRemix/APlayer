package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.viewpager.widget.ViewPager
import remix.myplayer.R
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import kotlin.math.abs

/**
 * Created by taeja on 16-1-25.
 */
class AudioViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs), Runnable {
  private var oldSpeed = 1f
  private var fastForward = false
  private var lastX = 0f
  private var lastY = 0f
  private val touchSlop by lazy {
    ViewConfiguration.get(context).scaledTouchSlop
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        if (ev.x <= width * 0.25 || ev.x >= width * 0.75) {
          postDelayed(this, 800)
          lastX = ev.x
          lastY = ev.y
        }
      }

      MotionEvent.ACTION_MOVE -> {
        if (abs(ev.x - lastX) >= touchSlop || abs(ev.y - lastY) >= touchSlop) {
          removeCallbacks(this)
        }
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        removeCallbacks(this)
        restore()
      }
    }

    return super.dispatchTouchEvent(ev)
  }

  override fun onTouchEvent(ev: MotionEvent): Boolean {
    if (ev.action == MotionEvent.ACTION_MOVE && fastForward) {
      return true
    }
    return super.onTouchEvent(ev)
  }

  private fun restore() {
    if (fastForward) {
      MusicServiceRemote.service?.setSpeed(oldSpeed)
      fastForward = false
    }
  }

  override fun run() {
    MusicServiceRemote.service?.apply {
      fastForward = true
      ToastUtil.show(context, getString(R.string.speed_at_2x))
      oldSpeed = speed
      setSpeed(2f)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    removeCallbacks(this)
  }

}
