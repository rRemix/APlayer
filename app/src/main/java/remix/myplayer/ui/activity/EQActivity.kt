package remix.myplayer.ui.activity

import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.appcompat.widget.AppCompatSeekBar
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import butterknife.ButterKnife
import kotlinx.android.synthetic.main.activity_eq.*
import remix.myplayer.R
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.util.ToastUtil

/**
 * Created by Remix on 19-5-6.
 */
class EQActivity : ToolbarActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val sessionId = MusicServiceRemote.getMediaPlayer()?.audioSessionId
    if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == null) {
      Toast.makeText(this, resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show()
      finish()
      return
    }

    // 初始化两次
    if (!EQHelper.init(this,sessionId,true)) {
      ToastUtil.show(this, R.string.eq_initial_failed)
      finish()
      return
    }

    setContentView(R.layout.activity_eq)
    ButterKnife.bind(this)

    setUpToolbar(getString(R.string.eq))

    setUpUI()

  }

  private fun setUpUI() {
    //初始化开关
    eq_switch.isEnabled = EQHelper.builtEqualizerInit
    eq_switch.isChecked = EQHelper.enable
    eq_switch.setOnCheckedChangeListener { buttonView, isChecked ->
      updateEnable(isChecked)
      eq_reset.isEnabled = EQHelper.enable
    }
    TintHelper.setTintAuto(eq_switch, ThemeStore.getAccentColor(), false)

    //初始化重置按钮背景
    TintHelper.setTintAuto(eq_reset, ThemeStore.getAccentColor(), false)
    eq_reset.isEnabled = EQHelper.enable

    val bandNumber = EQHelper.bandNumber
    val accentColor = ThemeStore.getAccentColor()

    for (i in 0 until bandNumber) {
      val eqLayout = LayoutInflater.from(this).inflate(R.layout.layout_eq_seekbar, eq_container, false)

      eqLayout.findViewById<TextView>(R.id.tv_freq).text = String.format("%d Hz", EQHelper.getCenterFreq(i))
      eqLayout.findViewById<TextView>(R.id.tv_min).text = (EQHelper.minLevel / 100).toString()
      eqLayout.findViewById<TextView>(R.id.tv_max).text = (EQHelper.maxLevel / 100).toString()

      val seekBarView = eqLayout.findViewById<AppCompatSeekBar>(R.id.eq_seekbar)
      seekBarView.tag = i
      seekBarView.max = EQHelper.maxLevel - EQHelper.minLevel
      seekBarView.progress = EQHelper.getBandLevel(i) - EQHelper.minLevel
      seekBarView.isEnabled = EQHelper.enable

      TintHelper.setTint(seekBarView, accentColor, false)

      seekBarView.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
          if (fromUser) {
            EQHelper.setBandLevel(i, progress + EQHelper.minLevel)
          }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
      })
      eq_container.addView(eqLayout)
    }

    //低音增强
    bass_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        EQHelper.bassBoostStrength = progress
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
      }

    })
    bass_seekbar.isEnabled = EQHelper.isBassBoostEnabled
    bass_seekbar.progress = EQHelper.bassBoostStrength
    TintHelper.setTint(bass_seekbar, accentColor, false)

    //环绕声
//    virtualizer_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//        EQHelper.virtualizerStrength = progress
//      }
//
//      override fun onStartTrackingTouch(seekBar: SeekBar?) {
//      }
//
//      override fun onStopTrackingTouch(seekBar: SeekBar?) {
//      }
//
//    })
//    virtualizer_seekbar.isEnabled = EQHelper.isVirtualizerEnabled
//    virtualizer_seekbar.progress = EQHelper.virtualizerStrength
    TintHelper.setTint(virtualizer_seekbar, accentColor, false)
  }


  private fun updateEnable(enable: Boolean) {
    EQHelper.updateEnable(enable)
    for (i in 0 until EQHelper.bandNumber) {
      val child = eq_container.findViewWithTag<View>(i)
      if (child is SeekBar) {
        child.isEnabled = enable
      }
    }

    bass_seekbar.isEnabled = EQHelper.isBassBoostEnabled
//    virtualizer_seekbar.isEnabled = EQHelper.isVirtualizerEnabled
  }

  //重置音效设置
  fun onReset(v: View) {
    EQHelper.reset()
    for (i in 0 until EQHelper.bandNumber) {
      val child = eq_container.findViewWithTag<View>(i)
      if (child is SeekBar) {
        child.progress = EQHelper.getBandLevel(i) - EQHelper.minLevel
      }
    }
    bass_seekbar.progress = 0
    virtualizer_seekbar.progress = 0
  }

}
