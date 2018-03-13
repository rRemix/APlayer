package remix.myplayer.service

import android.os.CountDownTimer
import android.os.Handler
import android.support.annotation.FloatRange

/**
 * Created by Remix on 2018/3/13.
 */

class VolumeController {
    private val mHandler: Handler = Handler()
    private val mFadeInRunnable: Runnable = Runnable {
        val mediaPlayer = MusicService.getMediaPlayer()
        object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
            override fun onFinish() {
                to(1)
            }

            override fun onTick(millisUntilFinished: Long) {
                val volume = 1f - millisUntilFinished * 1.0f / DURATION_IN_MS
                mediaPlayer?.setVolume(volume, volume)
            }
        }.start()
    }
    private val mFadeOutRunnable: Runnable = Runnable {
        val mediaPlayer = MusicService.getMediaPlayer()
        object : CountDownTimer(DURATION_IN_MS,DURATION_IN_MS / 10){
            override fun onTick(millisUntilFinished: Long) {
                val volume = millisUntilFinished * 1.0f / DURATION_IN_MS
                mediaPlayer?.setVolume(volume,volume)
            }

            override fun onFinish() {
                to(0)
                mediaPlayer?.pause()
            }

        }.start()
    }

    fun to(@FloatRange(from = 0.0, to = 1.0) toVolume: Float){
        to(toVolume,toVolume)
    }

    fun to(@FloatRange(from = 0.0, to = 1.0) leftVolume: Float, @FloatRange(from = 0.0, to = 1.0) rightVolume: Float){
        val mediaPlayer = MusicService.getMediaPlayer()
        mediaPlayer.setVolume(leftVolume,rightVolume)
    }

    /**
     * 淡入
     */
    fun fadeIn(){
        mHandler.removeCallbacks(mFadeInRunnable)
        mHandler.removeCallbacks(mFadeOutRunnable)
        mHandler.post(mFadeInRunnable)
    }

    /**
     * 淡出
     */
    fun fadeOut(){
        mHandler.removeCallbacks(mFadeInRunnable)
        mHandler.removeCallbacks(mFadeOutRunnable)
        mHandler.post(mFadeOutRunnable)
    }

    companion object {
        private val DURATION_IN_MS = 600L
    }
}