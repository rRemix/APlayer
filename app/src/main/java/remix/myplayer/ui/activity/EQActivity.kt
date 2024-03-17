package remix.myplayer.ui.activity

import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import remix.myplayer.R
import remix.myplayer.databinding.ActivityEqBinding
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
  private lateinit var binding: ActivityEqBinding

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

    binding = ActivityEqBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setUpToolbar(getString(R.string.eq))

    setUpUI()

  }

  private fun setUpUI() {
    //初始化开关
    binding.eqSwitch.isEnabled = EQHelper.builtEqualizerInit
    binding.eqSwitch.isChecked = EQHelper.enable
    binding.eqSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
      updateEnable(isChecked)
      binding.eqReset.isEnabled = EQHelper.enable
    }
    TintHelper.setTintAuto(binding.eqSwitch, ThemeStore.accentColor, false)

    //初始化重置按钮背景
    TintHelper.setTintAuto(binding.eqReset, ThemeStore.accentColor, false)
    binding.eqReset.isEnabled = EQHelper.enable

    val bandNumber = EQHelper.bandNumber
    val accentColor = ThemeStore.accentColor

    val decimalFormat = DecimalFormat("+#;-#")
    val minLevelText = decimalFormat.format(EQHelper.minLevel / 100)
    val maxLevelText = decimalFormat.format(EQHelper.maxLevel / 100)

    for (i in 0 until bandNumber) {
      val layout = LayoutEqSeekbarBinding.inflate(layoutInflater, binding.eqContainer, false)

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

      binding.eqContainer.addView(layout.root)
    }

    //低音增强
    binding.bassSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        EQHelper.bassBoostStrength = progress
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
      }

    })
    binding.bassSeekbar.isEnabled = EQHelper.isBassBoostEnabled
    binding.bassSeekbar.progress = EQHelper.bassBoostStrength
    TintHelper.setTint(binding.bassSeekbar, accentColor, false)

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
    TintHelper.setTint(binding.virtualizerSeekbar, accentColor, false)
  }


  private fun updateEnable(enable: Boolean) {
    EQHelper.updateEnable(enable)
    for (i in 0 until EQHelper.bandNumber) {
      val child = binding.eqContainer.findViewWithTag<View>(i)
      if (child is SeekBar) {
        child.isEnabled = enable
      }
    }

    binding.bassSeekbar.isEnabled = EQHelper.isBassBoostEnabled
//    virtualizer_seekbar.isEnabled = EQHelper.isVirtualizerEnabled
  }

  //重置音效设置
  fun onReset(v: View) {
    EQHelper.reset()
    for (i in 0 until EQHelper.bandNumber) {
      val child = binding.eqContainer.findViewWithTag<View>(i)
      if (child is SeekBar) {
        child.progress = EQHelper.getBandLevel(i) - EQHelper.minLevel
      }
    }
    binding.bassSeekbar.progress = 0
    binding.virtualizerSeekbar.progress = 0
  }

}
