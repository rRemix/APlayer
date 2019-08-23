package remix.myplayer.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.widget.Toast
import com.tencent.bugly.crashreport.CrashReport
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.ui.activity.EQActivity
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY.*
import remix.myplayer.util.Util.isIntentAvailable
import timber.log.Timber

/**
 * created by Remix on 2019-05-06
 */
object EQHelper {

  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null

  private val bandLevels = ArrayList<Short>()

  var bandNumber: Short = 0
  var maxLevel: Short = 0
  var minLevel: Short = 0

  var enable = false

  //  var systemSessionOpen = false
//  var builtIdSessionOpen = false
  var builtEqualizerInit = false

  val isBassBoostEnabled: Boolean
    get() = enable && bassBoost?.strengthSupported == true

  var bassBoostStrength: Int
    get() = SPUtil.getValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, 0)
    set(strength) {
      SPUtil.putValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, strength)
      if (isBassBoostEnabled) {
        tryRun({
          bassBoost?.setStrength(strength.toShort())
        }, {
          releaseBassBoost()
        })
      }
    }

//  val isVirtualizerEnabled: Boolean
//    get() = enable && virtualizer?.strengthSupported == true
//
//  var virtualizerStrength: Int
//    get() = SPUtil.getValue(App.getContext(), NAME, VIRTUALIZER_STRENGTH, 0)
//    set(strength) {
//      SPUtil.putValue(App.getContext(), NAME, VIRTUALIZER_STRENGTH, strength)
//      if (isVirtualizerEnabled) {
//        virtualizer?.setStrength(strength.toShort())
//      }
//    }

  fun init(context: Context, sessionId: Int, force: Boolean = false): Boolean {
    Timber.v("init, audioSessionId: $sessionId")

    if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
      return false
    }

    //系统均衡器不可用 才初始化内置的
    if (isSystemEqualizerAvailable(context)) {
      return false
    }

    //是否启用音效设置
    enable = SPUtil.getValue(App.getContext(), NAME, ENABLE_EQ, false)
    // 不需要初始化
    if (!enable && !force) {
      builtEqualizerInit = false
      return builtEqualizerInit
    }

    Timber.v("init start")

    tryRun({
      equalizer = Equalizer(0, sessionId)
      equalizer?.also { equalizer ->
        equalizer.enabled = enable

        //得到当前Equalizer引擎所支持的控制频率的标签数目。
        bandNumber = equalizer.numberOfBands

        //最小范围
        minLevel = equalizer.bandLevelRange[0]
        //最大范围
        maxLevel = equalizer.bandLevelRange[1]

        //得到当前Equalizer引擎所支持的控制频率的标签数目。
        bandNumber = equalizer.numberOfBands

        //得到之前存储的每个频率的db值
        for (i in 0 until bandNumber) {
          val bangLevel = SPUtil.getValue(App.getContext(), NAME, "band$i", 0)
          bandLevels.add(bangLevel.toShort())
        }

        Timber.v("init finish")

        builtEqualizerInit = true
      }
    }, {
      equalizer = null
      builtEqualizerInit = false
      Timber.v("init failed")
    })

    return builtEqualizerInit
  }

  fun open(context: Context, audioSessionId: Int) {
    Timber.v("open, audioSessionId: $audioSessionId")

    if (audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
      return
    }

    if (isSystemEqualizerAvailable(context)) {
      openSystemAudioEffectSession(context, audioSessionId)
    } else {
      Timber.v("open built-in")
      //是否启用音效设置
      enable = SPUtil.getValue(App.getContext(), NAME, ENABLE_EQ, false)
      if (!enable) {
        return
      }
      tryRun({
        //EQ
        equalizer?.release()

        equalizer = Equalizer(0, audioSessionId)
        equalizer?.also { equalizer ->
          equalizer.enabled = enable

          //得到之前存储的每个频率的db值
          for (i in 0 until bandNumber) {
            if (enable) {
              equalizer.setBandLevel(i.toShort(), bandLevels[i])
            }
          }
        }
      }, {
        releaseEqualizer()
      })

      tryRun({
        //低音增强
        bassBoost?.release()
        bassBoost = BassBoost(0, audioSessionId)
        bassBoost?.also { bassBoost ->
          bassBoost.enabled = enable && bassBoost.strengthSupported
          if (bassBoost.enabled) {
            bassBoost.setStrength(bassBoostStrength.toShort())
          }
        }
      }, {
        releaseBassBoost()
      })


      //环绕声
//      virtualizer?.enabled = false
//      virtualizer?.canVirtualize()
//
//      virtualizer = Virtualizer(0, audioSessionId)
//      virtualizer?.also { virtualizer ->
//        try {
//          virtualizer.enabled = enable && virtualizer.strengthSupported
//          if (virtualizer.enabled) {
//            virtualizer.setStrength(virtualizerStrength.toShort())
//          }
//        } catch (e: Exception) {
//          Timber.w(e)
//        }
//      }

      Timber.v("min: $minLevel max: $maxLevel bandNumber: $bandNumber")
    }
  }

  fun close(context: Context, audioSessionId: Int) {
    Timber.v("close")

    tryRun({
      releaseEqualizer()
    }, {

    })

    tryRun({
      releaseBassBoost()
    }, {

    })

    closeSystemAudioEffectSession(context, audioSessionId)
  }


  fun getCenterFreq(band: Int): Int {
    return equalizer?.getCenterFreq(band.toShort()) ?: 0
  }

  fun getBandLevel(band: Int): Short {
    return bandLevels[band]
  }

  fun setBandLevel(band: Int, level: Int) {
    tryRun({
      equalizer?.setBandLevel(band.toShort(), level.toShort())
    }, {
      releaseEqualizer()
    })
    bandLevels[band] = level.toShort()
    SPUtil.putValue(App.getContext(), NAME, "band$band", level)
  }

  fun updateEnable(enable: Boolean) {
    this.enable = enable
    SPUtil.putValue(App.getContext(), NAME, ENABLE_EQ, enable)

    tryRun({
      equalizer?.enabled = enable
      for (band in 0 until bandNumber) {
        val bandLevel = if (enable) getBandLevel(band) else 0
        equalizer?.setBandLevel(band.toShort(), bandLevel)
      }
    }, {
      releaseEqualizer()
    })

    tryRun({
      bassBoost?.enabled = isBassBoostEnabled
      bassBoost?.setStrength(if (isBassBoostEnabled) bassBoostStrength.toShort() else 0)
    }, {
      releaseBassBoost()
    })

//    try {
//      virtualizer?.enabled = isVirtualizerEnabled
//      virtualizer?.setStrength(if (isVirtualizerEnabled) virtualizerStrength.toShort() else 0)
//    } catch (e: Exception) {
//      Timber.w(e)
//    }

  }

  private fun releaseBassBoost() {
    bassBoost?.release()
    bassBoost = null
  }

  private fun releaseEqualizer() {
    equalizer?.release()
    equalizer = null
  }

  fun reset() {
    for (i in 0 until bandNumber) {
      setBandLevel(i, 0)
    }

    bassBoostStrength = 0
//    virtualizerStrength = 0
  }

  private fun openSystemAudioEffectSession(context: Context, audioSessionId: Int) {
    val audioEffectsIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    context.sendBroadcast(audioEffectsIntent)
  }

  private fun closeSystemAudioEffectSession(context: Context, audioSessionId: Int) {
    val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    context.sendBroadcast(audioEffectsIntent)
  }

  /**
   * 启动均衡器
   */
  @JvmStatic
  fun startEqualizer(activity: Activity) {
    val sessionId = MusicServiceRemote.getMediaPlayer()?.audioSessionId
    if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
      Toast.makeText(activity, activity.resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show()
      return
    }
    val audioEffectIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
    audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    if (isSystemEqualizerAvailable(activity)) {
      activity.startActivityForResult(audioEffectIntent, REQUEST_EQ)
    } else {
      activity.startActivity(Intent(activity, EQActivity::class.java))
    }
  }

  private fun isSystemEqualizerAvailable(context: Context): Boolean {
    return isIntentAvailable(context, Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL))
  }

  private fun tryRun(block: () -> Unit, error: () -> Unit) {
    try {
      block()
    } catch (e: Exception) {
      Timber.w(e)
      error()
      CrashReport.postCatchedException(e)
    } finally {
    }
  }


  const val REQUEST_EQ = 0


}