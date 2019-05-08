package remix.myplayer.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import android.widget.Toast
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.ui.activity.EQActivity
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY.*
import remix.myplayer.util.Util.isIntentAvailable
import timber.log.Timber
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * created by Remix on 2019-05-06
 */
object EQHelper {

  private lateinit var equalizer: Equalizer
  private lateinit var bassBoost: BassBoost
  private lateinit var virtualizer: Virtualizer

  private val bandLevels = ArrayList<Short>()

  var bandNumber: Short = 0
  var maxLevel: Short = 0
  var minLevel: Short = 0

  var enable = false

  var systemSessionOpen = false
  var builtIdSessionOpen = false

  val isBassBoostEnabled: Boolean
    get() = enable && bassBoost.strengthSupported

  var bassBoostStrength: Int
    get() = SPUtil.getValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, 0)
    set(strength) {
      SPUtil.putValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, strength)
      if (isBassBoostEnabled) {
        bassBoost.setStrength(strength.toShort())
      }
    }

  val isVirtualizerEnabled: Boolean
    get() = enable && virtualizer.strengthSupported

  var virtualizerStrength: Int
    get() = SPUtil.getValue(App.getContext(), NAME, VIRTUALIZER_STRENGTH, 0)
    set(strength) {
      SPUtil.putValue(App.getContext(), NAME, VIRTUALIZER_STRENGTH, strength)
      if (isVirtualizerEnabled) {
        virtualizer.setStrength(strength.toShort())
      }
    }


  fun open(context: Context, mediaPlayer: IjkMediaPlayer) {
    val audioSessionId = mediaPlayer.audioSessionId
    Timber.v("open, audioSessionId: $audioSessionId")

    if (audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
      return
    }

    if (isSystemAudioEffectAvailable(context)) {
      openSystemAudioEffectSession(context, audioSessionId)
    } else {
      //是否启用音效设置
      enable = SPUtil.getValue(App.getContext(), NAME, ENABLE_EQ, false)

      //EQ
      equalizer = Equalizer(0, audioSessionId)
      equalizer.enabled = enable

      //得到当前Equalizer引擎所支持的控制频率的标签数目。
      bandNumber = equalizer.numberOfBands

      //得到之前存储的每个频率的db值
      for (i in 0 until bandNumber) {
        val bangLevel = SPUtil.getValue(App.getContext(), NAME, "band$i", 0)
        bandLevels.add(bangLevel.toShort())
        if (enable) {
          equalizer.setBandLevel(i.toShort(), bangLevel.toShort())
        }
      }

      //最小范围
      minLevel = equalizer.bandLevelRange[0]
      //最大范围
      maxLevel = equalizer.bandLevelRange[1]

      //低音增强
      bassBoost = BassBoost(0, audioSessionId)
      bassBoost.enabled = enable && bassBoost.strengthSupported
      if (bassBoost.enabled) {
        bassBoost.setStrength(bassBoostStrength.toShort())
      }

      //环绕声
      virtualizer = Virtualizer(0, audioSessionId)
      virtualizer.enabled = enable && virtualizer.strengthSupported
      if (virtualizer.enabled) {
        virtualizer.setStrength(virtualizerStrength.toShort())
      }

      //初始化完成
      builtIdSessionOpen = true

      Timber.v("min: $minLevel max: $maxLevel bandNumber: $bandNumber")
    }
  }

  fun close(context: Context, mediaPlayer: IjkMediaPlayer) {
    Timber.v("close")
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mediaPlayer.audioSessionId)
    if (isIntentAvailable(context, intent)) {
      closeSystemAudioEffectSession(context, mediaPlayer.audioSessionId)
    } else {
      equalizer.enabled = false
      equalizer.release()
    }
    builtIdSessionOpen = false
    systemSessionOpen = false
  }


  fun getCenterFreq(band: Int): Int {
    return equalizer.getCenterFreq(band.toShort())
  }

  fun getBandLevel(band: Int): Short {
    return bandLevels[band]
  }

  fun setBandLevel(band: Int, level: Int) {
    equalizer.setBandLevel(band.toShort(), level.toShort())
    bandLevels[band] = level.toShort()
    SPUtil.putValue(App.getContext(), NAME, "band$band", level)
  }

  fun updateEnable(enable: Boolean) {
    this.enable = enable
    SPUtil.putValue(App.getContext(), NAME, ENABLE_EQ, enable)

    equalizer.enabled = enable
    for (band in 0 until bandNumber) {
      val bandLevel = if (enable) getBandLevel(band) else 0
      equalizer.setBandLevel(band.toShort(), bandLevel)
    }

    bassBoost.enabled = isBassBoostEnabled
    bassBoost.setStrength(if (isBassBoostEnabled) bassBoostStrength.toShort() else 0)

    virtualizer.enabled = isVirtualizerEnabled
    virtualizer.setStrength(if (isVirtualizerEnabled) virtualizerStrength.toShort() else 0)

  }

  fun reset() {
    for (i in 0 until bandNumber) {
      setBandLevel(i, 0)
    }

    bassBoostStrength = 0
    virtualizerStrength = 0
  }

  private fun openSystemAudioEffectSession(context: Context, audioSessionId: Int) {
    val audioEffectsIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    context.sendBroadcast(audioEffectsIntent)
    systemSessionOpen = true
  }

  private fun closeSystemAudioEffectSession(context: Context, audioSessionId: Int) {
    val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    context.sendBroadcast(audioEffectsIntent)
    systemSessionOpen = false
  }

  /**
   * 启动均衡器
   */
  @JvmStatic
  fun startEqualizer(activity: Activity) {
    val sessionId = MusicServiceRemote.getMediaPlayer()?.getAudioSessionId()
    if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
      Toast.makeText(activity, activity.resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG)
          .show()
      return
    }
    val audioEffectIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
    audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    if (isSystemAudioEffectAvailable(activity)) {
      activity.startActivityForResult(audioEffectIntent, REQUEST_EQ)
    } else {
      activity.startActivity(Intent(activity, EQActivity::class.java))
    }
  }

  private fun isSystemAudioEffectAvailable(context: Context): Boolean {
    return false
//    return isIntentAvailable(context, Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL))
  }

  const val REQUEST_EQ = 0


}