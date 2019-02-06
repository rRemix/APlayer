package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.ACTION_CMD
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by taeja on 16-2-5.
 */

/**
 * 接收线控的广播
 */
class MediaButtonReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {

    if (handleMediaButtonIntent(context, intent)) {
      Timber.v("onReceive")
      abortBroadcast()
    }
  }

  companion object {
    const val TAG = "MediaButtonReceiver"
    //按下了几次
    private var clickCount = 0

    @JvmStatic
    fun handleMediaButtonIntent(context: Context, intent: Intent?): Boolean {
      Timber.v("handleMediaButtonIntent")
      if (intent == null)
        return false
      val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
      //过滤按下事件
      val isActionUp = event.action == KeyEvent.ACTION_UP
      if (!isActionUp) {
        return true
      }

      val keyCode = event.keyCode
      if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
          keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
          keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
        val ctrlIntent = Intent(ACTION_CMD)

        ctrlIntent.putExtra(EXTRA_CONTROL, when (keyCode) {
          KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_PAUSE -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_PLAY -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_NEXT -> Command.NEXT
          KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Command.PREV
          else -> -1
        })
        Timber.v("sendLocalBroadcast: $ctrlIntent")
        sendLocalBroadcast(ctrlIntent)
        return true
      }


      //如果是第一次按下，开启一条线程去判断用户操作
      if (clickCount == 0) {
        object : Thread() {
          override fun run() {
            try {
              Thread.sleep(800)
              val action = Intent(MusicService.ACTION_CMD)
              action.putExtra(EXTRA_CONTROL, when (clickCount) {
                1 -> Command.TOGGLE
                2 -> Command.NEXT
                3 -> Command.PREV
                else -> -1
              })
              sendLocalBroadcast(action)
              Timber.v("count=$clickCount")
              clickCount = 0
            } catch (e: InterruptedException) {
              e.printStackTrace()
            }

          }
        }.start()
      }
      clickCount++
      return true
    }
  }
}
