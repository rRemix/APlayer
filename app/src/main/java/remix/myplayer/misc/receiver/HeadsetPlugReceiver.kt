package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.Command
import remix.myplayer.util.SPUtil
import remix.myplayer.util.Util.sendCMDLocalBroadcast
import timber.log.Timber

/**
 * Created by Remix on 2016/3/23.
 */

/**
 * 接收耳机插入与拔出的广播 当检测到耳机拔出并且正在播放时，发送停止播放的广播
 */
class HeadsetPlugReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) {
      return
    }

    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
      Timber.v("becoming noise")
      sendCMDLocalBroadcast(Command.PAUSE)
      return
    }

    val name = intent.getStringExtra("name")
    val microphone = intent.getIntExtra("microphone", -1)
    val state = intent.getIntExtra("state", -1)
    Timber.v("state: $state name: $name mic: $microphone")

    if (state == PLUGGED) {
      Timber.v("耳机插入")
      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.AUTO_PLAY, NEVER) == HEADSET_PLUG) {
        sendCMDLocalBroadcast(Command.START)
      }
    } else if (state == UNPLUGGED && MusicServiceRemote.isPlaying()) {
      Timber.v("耳机拔出")
      sendCMDLocalBroadcast(Command.PAUSE)

    }
  }

  companion object {
    const val UNPLUGGED = 0
    const val PLUGGED = 1

    const val HEADSET_PLUG = 0
    const val OPEN_SOFTWARE = 1
    const val NEVER = 2
  }
}
