package remix.myplayer.ui.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import java.util.Timer;
import java.util.TimerTask;
import remix.myplayer.R;
import remix.myplayer.helper.SleepTimer;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.theme.TintHelper;
import remix.myplayer.ui.dialog.base.BaseDialog;
import remix.myplayer.ui.widget.CircleSeekBar;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseDialog {

  private static final String EXTRA_MINUTE = "Minute";
  private static final String EXTRA_SECOND = "Second";

  public static TimerDialog newInstance() {
    TimerDialog timerDialog = new TimerDialog();
    return timerDialog;
  }

  //提示信息
  @BindView(R.id.timer_info_container)
  View mInfoContainer;
  @BindView(R.id.timer_content_container)
  View mContentContainer;
  //分钟
  @BindView(R.id.minute)
  TextView mMinute;
  //秒
  @BindView(R.id.second)
  TextView mSecond;
  //设置或取消默认
  SwitchCompat mSwitch;
  //圆形seekbar
  @BindView(R.id.close_seekbar)
  CircleSeekBar mSeekbar;
  //开始或取消计时
  @BindView(R.id.close_toggle)
  TextView mToggle;
  @BindView(R.id.close_stop)
  TextView mCancel;


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
    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
        .customView(R.layout.dialog_timer, false)
        .theme(com.afollestad.materialdialogs.Theme.LIGHT)
        .build();

    View root = dialog.getCustomView();
    ButterKnife.bind(this, root);

    mHandler = new MsgHandler(this);

    //如果正在计时，设置seekbar的进度
    if (SleepTimer.isTicking()) {
      mTime = (int) (SleepTimer.getMillisUntilFinish() / 1000);
      if (mTime > 0) {
        mSeekbar.setProgress(mTime);
        mSeekbar.setStart(true);
      }
    }

    mSeekbar.setOnSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser) {
        if (progress > 0) {
          //记录倒计时时间和更新界面
          int minute = (int) (progress / 60);
          mMinute.setText(minute < 10 ? "0" + minute : "" + minute);
          mSecond.setText("00");
          //取整数分钟
          mTime = minute * 60;
          mSaveTime = minute * 60;
        }
      }

      @Override
      public void onStartTrackingTouch(CircleSeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(CircleSeekBar seekBar) {
      }
    });

    //初始化switch
    mSwitch = new SwitchCompat(getContext());
    TintHelper.setTintAuto(mSwitch, ThemeStore.getAccentColor(), false);
    ((LinearLayout) root.findViewById(R.id.popup_timer_container)).addView(mSwitch);

    //读取保存的配置
    boolean hasDefault = SPUtil
        .getValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false);
    final int time = SPUtil
        .getValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);

    //默认选项
    if (hasDefault && time > 0) {
      //如果有默认设置并且没有开始计时，直接开始计时
      //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
      if (!SleepTimer.isTicking()) {
        mTime = time;
        toggle();
      }
    }
    mSwitch.setChecked(hasDefault);
    mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        if (mSaveTime > 0) {
          ToastUtil.show(getContext(), R.string.set_success);
          SPUtil
              .putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, true);
          SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION,
              mSaveTime);
        } else {
          ToastUtil.show(getContext(), R.string.plz_set_correct_time);
          mSwitch.setChecked(false);
        }
      } else {
        ToastUtil.show(getContext(), R.string.cancel_success);
        SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false);
        SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);
      }
    });

    mToggle.setText(SleepTimer.isTicking() ? R.string.cancel_timer : R.string.start_timer);

    //分钟 秒 背景框
    ButterKnife.apply(new View[]{root.findViewById(R.id.timer_minute_container),
            root.findViewById(R.id.timer_second_container)},
        (view, index) -> {
          view.setBackground(new GradientDrawableMaker()
              .color(Color.TRANSPARENT)
              .corner(DensityUtil.dip2px(1))
              .strokeSize(DensityUtil.dip2px(1))
              .strokeColor(ColorUtil.getColor(R.color.timer_text_color))
              .make());
        });

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

  @OnClick({R.id.timer_info, R.id.timer_info_close, R.id.close_stop, R.id.close_toggle})
  public void OnClick(View view) {
    switch (view.getId()) {
      case R.id.timer_info:
        mContentContainer.setVisibility(View.INVISIBLE);
        mInfoContainer.setVisibility(View.VISIBLE);
        break;
      case R.id.timer_info_close:
        mInfoContainer.setVisibility(View.INVISIBLE);
        mContentContainer.setVisibility(View.VISIBLE);
        break;
      case R.id.close_stop:
        dismiss();
        break;
      case R.id.close_toggle:
        toggle();
        break;
    }
  }

  @OnHandleMessage
  public void handlerInternal(Message msg) {
    if (msg != null) {
      if (msg.getData() != null) {
        mMinute.setText(msg.getData().getString(EXTRA_MINUTE));
        mSecond.setText(msg.getData().getString(EXTRA_SECOND));
      }
      mSeekbar.setProgress(msg.arg1);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
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
