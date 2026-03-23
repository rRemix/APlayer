package remix.myplayer.service

import android.content.Context
import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFocusManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {

  private var callbacks: Callbacks? = null

  interface Callbacks {

    // 获得焦点
    fun onFocusGained()

    // 暂停
    fun onFocusLost()

    // 短暂暂停
    fun onFocusLostTransient()

    // 减小音量
    fun onFocusDuck()
  }

  private val audioManager: AudioManager =
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private val audioAttributes = AudioAttributesCompat.Builder()
    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
    .build()

  private val focusRequest: AudioFocusRequestCompat =
    AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
      .setAudioAttributes(audioAttributes)
      .setOnAudioFocusChangeListener { change ->
        Timber.v("onAudioFocusChange: %d", change)
        when (change) {
          AudioManager.AUDIOFOCUS_GAIN -> callbacks?.onFocusGained()
          AudioManager.AUDIOFOCUS_LOSS -> callbacks?.onFocusLost()
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> callbacks?.onFocusLostTransient()
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> callbacks?.onFocusDuck()
        }
      }
      .build()

  fun requestFocus(): Boolean {
    val result = AudioManagerCompat.requestAudioFocus(audioManager, focusRequest)
    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  fun abandonFocus() {
    AudioManagerCompat.abandonAudioFocusRequest(audioManager, focusRequest)
  }

  fun attach(callbacks: Callbacks) {
    this.callbacks = callbacks
  }

  fun detach() {
    callbacks = null
    abandonFocus()
  }

  fun shouldPauseForPhoneCall(): Boolean {
    val mode = audioManager.mode
    val inCall = mode == AudioManager.MODE_IN_CALL
        || mode == AudioManager.MODE_IN_COMMUNICATION
        || mode == AudioManager.MODE_RINGTONE
    Timber.v("shouldPauseForPhoneCall mode: $mode inCall: $inCall")
    return inCall
  }
}
