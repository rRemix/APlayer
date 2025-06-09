package remix.myplayer.helper

import android.os.CountDownTimer
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.util.ToastUtil
import timber.log.Timber

class SleepTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {

  var isFinished = false
    private set

  override fun onFinish() {
    isFinished = true
    callbacks.forEach {
      it.onFinish()
    }
  }

  override fun onTick(millisUntilFinished: Long) {
    millisUntilFinish = millisUntilFinished
  }

  fun revert() {
    callbacks.forEach {
      it.revert()
    }
  }

  interface Callback {
    fun onFinish()
    fun revert()
  }

  companion object {
    /** 定时关闭剩余时间 */
    @JvmStatic
    private var millisUntilFinish: Long = 0

    private var running = false

    @JvmStatic
    fun getMillisUntilFinish(): Long {
      return millisUntilFinish
    }

    @JvmStatic
    private var instance: SleepTimer? = null

    private val callbacks: MutableList<Callback> by lazy { ArrayList<Callback>() }

    @JvmStatic
    fun isTicking(): Boolean {
      return running
    }

    /**
     * 开始或者停止计时
     * @param start
     * @param duration
     */
    @JvmStatic
    fun toggleTimer(duration: Long) {
      val context = App.context
      val start = instance == null
      if (start) {
        if (duration <= 0) {
          ToastUtil.show(context, R.string.plz_set_correct_time)
          return
        }
        instance = SleepTimer(duration, 1000)
        instance?.start()
      } else {
        if (instance != null) {
          val isFinished = instance?.isFinished
          if (isFinished == true) {
            instance?.revert()
          } else {
            instance?.cancel()
          }
          instance = null
        }
        millisUntilFinish = 0
      }
      running = start
      ToastUtil.show(context, if (!start) context.getString(R.string.cancel_timer) else context.getString(R.string.will_stop_at_x, Math.ceil((duration / 1000 / 60).toDouble()).toInt()))
    }

    fun addCallback(callback: Callback) {
      callbacks.add(callback)
    }

  }
}