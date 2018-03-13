package remix.myplayer.service

import android.os.CountDownTimer

import java.util.Timer
import java.util.TimerTask

/**
 * Created by Remix on 2018/3/13.
 */

class Demo {
    internal fun test() {
        object : CountDownTimer(2000, 100) {

            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {

            }
        }.start()
    }
}
