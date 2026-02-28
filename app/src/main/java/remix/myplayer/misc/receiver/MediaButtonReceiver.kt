package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private const val MULTI_CLICK_WINDOW_MS = 300L

    // 按下了几次
    private var clickCount = 0
    private val clickLock = Any()
    private val clickHandler = Handler(Looper.getMainLooper())
    private var pendingClickRunnable: Runnable? = null

    @JvmStatic
    fun handleMediaButtonIntent(context: Context, intent: Intent?): Boolean {
      Timber.v("handleMediaButtonIntent")
      if (intent == null || intent.action != Intent.ACTION_MEDIA_BUTTON) {
        return false
      }

      val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
      } ?: return false
      when (event.action) {
        KeyEvent.ACTION_DOWN -> return true
        KeyEvent.ACTION_UP -> Unit
        else -> return false
      }

      when (event.keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
          sendCommand(Command.PLAY_PAUSE)
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PLAY -> {
          sendCommand(Command.PLAY)
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
          sendCommand(Command.PAUSE)
          return true
        }

        KeyEvent.KEYCODE_MEDIA_NEXT -> {
          sendCommand(Command.SKIP_TO_NEXT)
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
          sendCommand(Command.SKIP_TO_PREVIOUS)
          return true
        }
      }

      // 仅保留耳机 Hook 多击识别，避免未知按键误触发播放控制
      if (event.keyCode != KeyEvent.KEYCODE_HEADSETHOOK) {
        return false
      }

      synchronized(clickLock) {
        clickCount++
        pendingClickRunnable?.let(clickHandler::removeCallbacks)
        pendingClickRunnable = Runnable {
          val count = synchronized(clickLock) {
            val result = clickCount
            clickCount = 0
            pendingClickRunnable = null
            result
          }
          val command = when (count) {
            1 -> Command.PLAY_PAUSE
            2 -> Command.SKIP_TO_NEXT
            3 -> Command.SKIP_TO_PREVIOUS
            else -> -1
          }
          if (command != -1) {
            sendCommand(command)
          }
          Timber.v("count=$count")
        }
        clickHandler.postDelayed(pendingClickRunnable!!, MULTI_CLICK_WINDOW_MS)
      }

      return true
    }

    private fun sendCommand(command: Int) {
      val intent = Intent(ACTION_CMD).putExtra(EXTRA_COMMAND, command)
      Timber.v("sendLocalBroadcast: $intent")
      sendLocalBroadcast(intent)
    }
  }
}
