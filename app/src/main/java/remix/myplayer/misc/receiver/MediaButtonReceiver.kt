package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.ACTION_CMD
import remix.myplayer.service.MusicService.Companion.EXTRA_COMMAND
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

    // 按下了几次
    private var clickCount = 0

    @JvmStatic
    fun handleMediaButtonIntent(context: Context, intent: Intent?): Boolean {
      Timber.v("handleMediaButtonIntent")
      if (intent == null) {
        return false
      }

      val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
      } ?: return false
      //过滤按下事件
      val isActionUp = event.action == KeyEvent.ACTION_UP
      if (!isActionUp) {
        return true
      }

      val keyCode = event.keyCode
      if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
      ) {
        val ctrlIntent = Intent(ACTION_CMD)

        ctrlIntent.putExtra(
          EXTRA_COMMAND, when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Command.PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PAUSE -> Command.PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY -> Command.PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_NEXT -> Command.SKIP_TO_NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Command.SKIP_TO_PREVIOUS
            else -> -1
          }
        )
        Timber.v("sendLocalBroadcast: $ctrlIntent")
        sendLocalBroadcast(ctrlIntent)
        return true
      }

      // 如果是第一次按下，开启一条线程去判断用户操作
      if (clickCount == 0) {
        object : Thread() {
          override fun run() {
            try {
              sleep(800)
              val action = Intent(ACTION_CMD)
              action.putExtra(
                EXTRA_COMMAND, when (clickCount) {
                  1 -> Command.PLAY_PAUSE
                  2 -> Command.SKIP_TO_NEXT
                  3 -> Command.SKIP_TO_PREVIOUS
                  else -> -1
                }
              )
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
