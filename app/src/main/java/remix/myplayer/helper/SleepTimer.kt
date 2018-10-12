package remix.myplayer.helper

import android.content.ComponentName
import android.content.Intent
import android.os.CountDownTimer
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.util.Constants
import remix.myplayer.util.ToastUtil

class SleepTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {
    override fun onFinish() {
        App.getContext().sendBroadcast(Intent(Constants.EXIT)
                .setComponent(ComponentName(App.getContext(), ExitReceiver::class.java)))
    }

    override fun onTick(millisUntilFinished: Long) {
        millisUntilFinish = millisUntilFinished
    }

    companion object {
        /** 定时关闭剩余时间 */
        @JvmStatic
        var millisUntilFinish: Long = 0

        @JvmStatic
        var instance: SleepTimer? = null

        @JvmStatic
        fun isTicking(): Boolean {
            return millisUntilFinish > 0
        }

        /**
         * 开始或者停止计时
         * @param start
         * @param duration
         */
        @JvmStatic
        fun toggleTimer(duration: Long) {
            val context = App.getContext()
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
                    instance?.cancel()
                    instance = null
                }
                millisUntilFinish = 0
            }
            ToastUtil.show(context, if (!start) context.getString(R.string.cancel_timer) else context.getString(R.string.will_stop_at_x, Math.ceil((duration / 1000 / 60).toDouble()).toInt()))
        }
    }
}