package remix.myplayer.ui.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import remix.myplayer.R;
import remix.myplayer.databinding.DialogTimerBinding;
import remix.myplayer.helper.SleepTimer;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.theme.TintHelper;
import remix.myplayer.ui.dialog.base.BaseDialog;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseDialog {
  private DialogTimerBinding binding;

  private static final String EXTRA_MINUTE = "Minute";
  private static final String EXTRA_SECOND = "Second";

  public static TimerDialog newInstance() {
    return new TimerDialog();
  }

  //定时时间 单位秒
  private int mTime;
  //设置的定时时间 用于保存默认设置
  private int mSaveTime = -1;
  //每一秒中更新数据
  private Timer mUpdateTimer;
  //更新seekbar与剩余时间
  private MsgHandler mHandler;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog dialog = Theme.getBaseDialog(getContext())
        .customView(R.layout.dialog_timer, false)
        .title(R.string.timer)
        .titleGravity(GravityEnum.CENTER)
        .positiveText(SleepTimer.isTicking() ? R.string.cancel_timer : R.string.start_timer)
        .negativeText(R.string.close)
        .onPositive((_dialog, which) -> toggle())
        .onNegative((_dialog, which) -> dismiss())
        .build();
    binding = DialogTimerBinding.bind(Objects.requireNonNull(dialog.getCustomView()));

    mHandler = new MsgHandler(this);

    //如果正在计时，设置seekbar的进度
    if (SleepTimer.isTicking()) {
      binding.closeSeekbar.setClickable(false);
      mTime = (int) (SleepTimer.getMillisUntilFinish() / 1000);
      binding.closeSeekbar.setProgress(mTime);
    } else {
      binding.closeSeekbar.setClickable(true);
    }

    binding.closeSeekbar.setOnSeekBarChangeListener((seekBar, progress, fromUser) -> {
      //记录倒计时时间和更新界面
      int minute = progress / 60;
      binding.minute.setText(minute < 10 ? "0" + minute : "" + minute);
      binding.second.setText("00");
      //取整数分钟
      mTime = minute * 60;
      mSaveTime = minute * 60;
    });

    //初始化switch
    TintHelper.setTintAuto(binding.timerDefaultSwitch, ThemeStore.getAccentColor(), false);
    TintHelper.setTintAuto(binding.timerPendingSwitch, ThemeStore.getAccentColor(), false);

    //读取保存的配置
    final boolean pendingClose = SPUtil
        .getValue(getContext(), SETTING_KEY.NAME, SETTING_KEY.TIMER_EXIT_AFTER_FINISH, false);
    final boolean hasDefault = SPUtil
        .getValue(getContext(), SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, false);
    final int time = SPUtil
        .getValue(getContext(), SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, -1);

    //默认选项
    if (hasDefault && time > 0) {
      //如果有默认设置并且没有开始计时，直接开始计时
      //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
      if (!SleepTimer.isTicking()) {
        mTime = time;
        toggle();
      }
    }

    binding.timerPendingSwitch.setChecked(pendingClose);
    binding.timerPendingSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> SPUtil.putValue(getContext(), SETTING_KEY.NAME,
            SETTING_KEY.TIMER_EXIT_AFTER_FINISH, isChecked));

    binding.timerDefaultSwitch.setChecked(hasDefault);
    binding.timerDefaultSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        if (mSaveTime > 0) {
          ToastUtil.show(getContext(), R.string.set_success);
          SPUtil
              .putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT,
                  true);
          SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION,
              mSaveTime);
        } else {
          ToastUtil.show(getContext(), R.string.plz_set_correct_time);
          binding.timerDefaultSwitch.setChecked(false);
        }
      } else {
        ToastUtil.show(getContext(), R.string.cancel_success);
        SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT,
            false);
        SPUtil
            .putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);
      }
    });

    //分钟 秒 背景框
    for (View view : new View[]{binding.timerMinuteContainer, binding.timerSecondContainer}) {
      view.setBackground(new GradientDrawableMaker()
          .color(Color.TRANSPARENT)
          .corner(DensityUtil.dip2px(1))
          .strokeSize(DensityUtil.dip2px(1))
          .strokeColor(Theme.resolveColor(getContext(), R.attr.text_color_secondary))
          .make());
    }

    //改变宽度
    Window window = dialog.getWindow();
    WindowManager.LayoutParams lp = window.getAttributes();
    lp.width = DensityUtil.dip2px(getContext(), 270);
    window.setAttributes(lp);

    return dialog;
  }


  /**
   * 根据是否已经开始计时来取消或开始计时
   */
  private void toggle() {
    if (mTime <= 0 && !SleepTimer.isTicking()) {
      ToastUtil.show(getContext(), R.string.plz_set_correct_time);
      return;
    }

    //如果开始计时，保存设置的时间
//        if(mIsTiming){
//            mSaveTime = mTime / 60;
//        }
    SleepTimer.toggleTimer(mTime * 1000);
    dismiss();
  }

  @OnHandleMessage
  public void handlerInternal(Message msg) {
    if (msg != null) {
      if (msg.getData() != null) {
        binding.minute.setText(msg.getData().getString(EXTRA_MINUTE));
        binding.second.setText(msg.getData().getString(EXTRA_SECOND));
      }
      binding.closeSeekbar.setProgress(msg.arg1);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    mHandler.remove();
    if (mUpdateTimer != null) {
      mUpdateTimer.cancel();
      mUpdateTimer = null;
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    if (SleepTimer.isTicking()) {
      mUpdateTimer = new Timer();
      mUpdateTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          int min, sec, remain;
          remain = (int) SleepTimer.getMillisUntilFinish() / 1000;
          min = remain / 60;
          sec = remain % 60;
          Message msg = new Message();
          msg.arg1 = remain;
          Bundle data = new Bundle();
          data.putString(EXTRA_MINUTE, min < 10 ? "0" + min : "" + min);
          data.putString(EXTRA_SECOND, sec < 10 ? "0" + sec : "" + sec);
          msg.setData(data);
          mHandler.sendMessage(msg);
        }
      }, 0, 1000);
    }
  }

}
