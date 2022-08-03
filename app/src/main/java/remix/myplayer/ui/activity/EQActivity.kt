package remix.myplayer.ui.activity

import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_eq.*
import remix.myplayer.R
import remix.myplayer.databinding.LayoutEqSeekbarBinding
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.util.ToastUtil
import java.text.DecimalFormat

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
    TintHelper.setTintAuto(eq_switch, ThemeStore.accentColor, false)

    //初始化重置按钮背景
    TintHelper.setTintAuto(eq_reset, ThemeStore.accentColor, false)
    eq_reset.isEnabled = EQHelper.enable

    val bandNumber = EQHelper.bandNumber
    val accentColor = ThemeStore.accentColor

    val decimalFormat = DecimalFormat("+#;-#")
    val minLevelText = decimalFormat.format(EQHelper.minLevel / 100)
    val maxLevelText = decimalFormat.format(EQHelper.maxLevel / 100)

    for (i in 0 until bandNumber) {
      val layout = LayoutEqSeekbarBinding.inflate(layoutInflater, eq_container, false)

      layout.tvFreq.text = String.format("%d mHz", EQHelper.getCenterFreq(i))
      layout.tvMin.text = minLevelText
      layout.tvMax.text = maxLevelText

      layout.eqSeekbar.tag = i
      layout.eqSeekbar.max = EQHelper.maxLevel - EQHelper.minLevel
      layout.eqSeekbar.progress = EQHelper.getBandLevel(i) - EQHelper.minLevel
      layout.eqSeekbar.isEnabled = EQHelper.enable
      TintHelper.setTint(layout.eqSeekbar, accentColor, false)
      layout.eqSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar,
            progress: Int,
            fromUser: Boolean
        ) {
          if (fromUser) {
            EQHelper.setBandLevel(i, progress + EQHelper.minLevel)
          }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
      })

      eq_container.addView(layout.root)
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
