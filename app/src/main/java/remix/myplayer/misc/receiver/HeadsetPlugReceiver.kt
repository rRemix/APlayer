package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.HEADSET_PLUG
import remix.myplayer.service.Command
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.util.Util.sendCMDLocalBroadcast
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Remix on 2016/3/23.
 */

/**
 * 接收耳机插入与拔出的广播 当检测到耳机拔出并且正在播放时，发送停止播放的广播
 */
@AndroidEntryPoint
class HeadsetPlugReceiver : BroadcastReceiver() {

  @Inject
  lateinit var settingPrefs: SettingPrefs

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
      if (settingPrefs.autoPlay == HEADSET_PLUG) {
        sendCMDLocalBroadcast(Command.PLAY)
      }
    } else if (state == UNPLUGGED && MusicStateSource.currentPlaybackUiState.isPlaying) {
      Timber.v("耳机拔出")
      sendCMDLocalBroadcast(Command.PAUSE)

    }
  }

  companion object {

    private const val UNPLUGGED = 0
    private const val PLUGGED = 1
  }
}
