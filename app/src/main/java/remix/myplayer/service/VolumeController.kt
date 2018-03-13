package remix.myplayer.service

import android.content.Context
import android.media.AudioManager
import android.os.CountDownTimer
import android.os.Handler

/**
 * Created by Remix on 2018/3/13.
 */
const val DURATION_IN_MS = 1000L
const val STUB_FADE_OUT = 1
const val STUB_FADE_IN = 2
class VolumeController(private val mService: MusicService){
    private val mHandler: Handler = Handler()

    fun to(toVolume: Float){
        to(toVolume,toVolume)
    }

    fun to(leftVolume: Float,rightVolume: Float){
        val mediaPlayer = MusicService.getMediaPlayer()
        mediaPlayer.setVolume(leftVolume,rightVolume)
    }

    fun fadeOut(){
        if(mHandler.hasMessages(STUB_FADE_OUT))
            return
        val mediaPlayer = MusicService.getMediaPlayer()
        mHandler.sendEmptyMessageDelayed(STUB_FADE_OUT, DURATION_IN_MS)
        object : CountDownTimer(DURATION_IN_MS, DURATION_IN_MS / 10) {
            override fun onFinish() {
                to(1)
            }

            override fun onTick(millisUntilFinished: Long) {
                val volume = 1f - millisUntilFinished * 1.0f / DURATION_IN_MS
                mediaPlayer?.setVolume(volume, volume)
            }
        }.cancel()
    }

    fun fadeIn(){
        if(mHandler.hasMessages(STUB_FADE_IN))
            return
        mHandler.sendEmptyMessageDelayed(STUB_FADE_IN, DURATION_IN_MS)
        val mediaPlayer = MusicService.getMediaPlayer()
        object : CountDownTimer(DURATION_IN_MS,DURATION_IN_MS / 10){
            override fun onTick(millisUntilFinished: Long) {
                val volume = millisUntilFinished * 1.0f / DURATION_IN_MS
                mediaPlayer?.setVolume(volume,volume)
                mediaPlayer?.pause()
            }

            override fun onFinish() {
                to(0)
            }

        }.start()
    }

}