package remix.myplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import com.afollestad.materialdialogs.GravityEnum
import remix.myplayer.R
import remix.myplayer.databinding.DialogTimerBinding
import remix.myplayer.helper.SleepTimer.Companion.getMillisUntilFinish
import remix.myplayer.helper.SleepTimer.Companion.isTicking
import remix.myplayer.helper.SleepTimer.Companion.toggleTimer
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.dialog.base.BaseDialog
import remix.myplayer.ui.widget.CircleSeekBar
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil
import java.util.*

/**
 * 定时关闭界面
 */
class TimerDialog : BaseDialog() {
  private var _binding: DialogTimerBinding? = null
  private val binding get() = _binding!!

  // 定时时间 单位秒
  private var time = 0

  // 每一秒中更新数据
  private var updateTimer: Timer? = null

  // 更新 seekbar 与剩余时间
  private var handler: MsgHandler? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog =
      Theme.getBaseDialog(context)
        .customView(R.layout.dialog_timer, false)
        .title(R.string.timer)
        .titleGravity(GravityEnum.CENTER)
        .positiveText(if (isTicking()) R.string.cancel_timer else R.string.start_timer)
        .negativeText(R.string.close)
        .onPositive { _, _ -> toggle() }
        .onNegative { _, _ -> dismiss() }
        .build()
    _binding = DialogTimerBinding.bind(dialog.customView!!)

    handler = MsgHandler(this)

    // 如果正在计时，设置 seekbar 的进度
    if (isTicking()) {
      binding.closeSeekbar.isClickable = false
      binding.closeSeekbar.progress = (getMillisUntilFinish() / 1000).toInt()
    } else {
      binding.closeSeekbar.isClickable = true
    }
    binding.closeSeekbar.onSeekBarChangeListener =
      object : CircleSeekBar.OnSeekBarChangeListener {
        @SuppressLint("SetTextI18n")
        override fun onProgressChanged(
          seekBar: CircleSeekBar?,
          progress: Int,
          fromUser: Boolean
        ) {
          // 记录倒计时时间和更新界面
          val minute = progress / 60
          binding.minute.text = if (minute < 10) "0$minute" else "$minute"
          binding.second.text = "00"
          time = minute * 60
        }
      }

    // 初始化 switch
    TintHelper.setTintAuto(binding.timerDefaultSwitch, accentColor, false)
    TintHelper.setTintAuto(binding.timerPendingSwitch, accentColor, false)

    // 读取保存的配置
    val exitAfterFinish = SPUtil.getValue(
      context, SETTING_KEY.NAME, SETTING_KEY.TIMER_EXIT_AFTER_FINISH, false
    )
    val hasDefault = SPUtil.getValue(
      context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, false
    )
    val savedTime =
      SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, -1)

    // 默认选项
    if (hasDefault && savedTime > 0) {
      // 如果有默认设置并且没有开始计时，直接开始计时
      // 如果有默认设置但已经开始计时，打开该 dialog，并更改 switch 外观
      if (!isTicking()) {
        time = savedTime
        toggle()
      }
    }

    binding.timerPendingSwitch.isChecked = exitAfterFinish
    binding.timerPendingSwitch.setOnCheckedChangeListener { _, isChecked ->
      SPUtil.putValue(
        context,
        SETTING_KEY.NAME,
        SETTING_KEY.TIMER_EXIT_AFTER_FINISH,
        isChecked
      )
    }
    binding.timerDefaultSwitch.isChecked = hasDefault
    binding.timerDefaultSwitch.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        if (time > 0) {
          ToastUtil.show(context, R.string.set_success)
          SPUtil.putValue(
            context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, true
          )
          SPUtil.putValue(
            context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, time
          )
        } else {
          ToastUtil.show(context, R.string.plz_set_correct_time)
          binding.timerDefaultSwitch.isChecked = false
        }
      } else {
        ToastUtil.show(context, R.string.cancel_success)
        SPUtil.putValue(
          context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, false
        )
        SPUtil.putValue(
          context, SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, -1
        )
      }
    }

    // 分 秒 背景框
    arrayOf(
      binding.timerMinuteContainer,
      binding.timerSecondContainer
    ).forEach {
      it.background = GradientDrawableMaker().color(Color.TRANSPARENT)
        .corner(DensityUtil.dip2px(1f).toFloat())
        .strokeSize(DensityUtil.dip2px(1f))
        .strokeColor(Theme.resolveColor(context, R.attr.text_color_secondary))
        .make()
    }

    // “设为默认”的帮助信息
    binding.timerDefaultInfo.setOnClickListener {
      Theme.getBaseDialog(context)
        .title(R.string.timer_default_info_title)
        .content(R.string.timer_default_info_content)
        .positiveText(R.string.close)
        .onPositive { dialog, _ -> dialog.dismiss() }
        .build()
        .show()
    }

    // 改变宽度
    val window = dialog.window!!
    val lp = window.attributes
    lp.width = DensityUtil.dip2px(context, 270F)
    window.attributes = lp

    return dialog
  }

  /**
   * 根据是否已经开始计时来取消或开始计时
   */
  private fun toggle() {
    if (time <= 0 && !isTicking()) {
      ToastUtil.show(context, R.string.plz_set_correct_time)
      return
    }
    toggleTimer((time * 1000).toLong())
    dismiss()
  }

  @OnHandleMessage
  fun handlerInternal(msg: Message?) {
    if (msg != null) {
      if (msg.data != null) {
        binding.minute.text = msg.data.getString(EXTRA_MINUTE)
        binding.second.text = msg.data.getString(EXTRA_SECOND)
      }
      binding.closeSeekbar.progress = msg.arg1
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    handler!!.remove()
    if (updateTimer != null) {
      updateTimer!!.cancel()
      updateTimer = null
    }
  }

  override fun onResume() {
    super.onResume()
    if (isTicking()) {
      updateTimer = Timer()
      updateTimer!!.schedule(object : TimerTask() {
        override fun run() {
          val remain: Int = getMillisUntilFinish().toInt() / 1000
          val min = remain / 60
          val sec = remain % 60
          val msg = Message()
          msg.arg1 = remain
          val data = Bundle()
          data.putString(EXTRA_MINUTE, if (min < 10) "0$min" else "$min")
          data.putString(EXTRA_SECOND, if (sec < 10) "0$sec" else "$sec")
          msg.data = data
          handler!!.sendMessage(msg)
        }
      }, 0, 1000)
    }
  }

  companion object {
    private const val EXTRA_MINUTE = "Minute"
    private const val EXTRA_SECOND = "Second"

    fun newInstance(): TimerDialog {
      return TimerDialog()
    }
  }
}