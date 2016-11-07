package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.service.TimerService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.ui.customview.CircleSeekBar;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseDialogActivity {

    //剩余时间
    @BindView(R.id.close_time)
    TextView mText;
    //设置或取下默认
    @BindView(R.id.popup_timer_switch)
    SwitchCompat mSwitch;
    //圆形seekbar
    @BindView(R.id.close_seekbar)
    CircleSeekBar mSeekbar;
    //开始或取消计时
    @BindView(R.id.close_toggle)
    Button mToggle;
    @BindView(R.id.close_stop)
    Button mCancel;

    //是否正在计时
    public static boolean misTiming = false;
    //是否正在运行
    public static boolean misRun = false;
    //定时时间
    private static long mTime;
    //更新seekbar与剩余时间
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mText.setText(msg.obj.toString());
            mSeekbar.setProgress(msg.arg1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"Timer");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_timer);
        ButterKnife.bind(this);

        //居中显示
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);

        //如果正在计时，设置seekbar的进度
        if(misTiming) {
            int remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
            mSeekbar.setProgress(remain / 60);
            mSeekbar.setStart(true);
        }

        mSeekbar.setOnSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser) {
                if (progress > 0) {
                    String text = (progress < 10 ? "0" + progress : "" + progress) + ":00min";
                    //记录倒计时时间和更新界面
                    mText.setText(text);
                    mTime = progress;
                }
            }
            @Override
            public void onStartTrackingTouch(CircleSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(CircleSeekBar seekBar) {
            }
        });

        //初始化开始计时按钮
        int[] state_prs = new int[]{android.R.attr.state_pressed};
        int[] state_non_focus = new int[]{-android.R.attr.state_focused};
        int color = ThemeStore.getStressColor();
        StateListDrawable bg = new StateListDrawable();
        bg.addState(state_prs,Theme.getCorner(1.0f,4,0,color));
        bg.addState(state_non_focus,Theme.getCorner(0.01f,4,1,color));

        mToggle.setBackground(bg);
        mToggle.setTextColor(new ColorStateList(new int[][]{state_prs,state_non_focus},
                new int[]{ColorUtil.getColor(R.color.white),color}));

        //初始化switch
//        mSwitch = findView(R.id.popup_timer_switch);
        mSwitch = new SwitchCompat(new ContextThemeWrapper(this, Theme.getTheme()));
        ((LinearLayout)findView(R.id.popup_timer_container)).addView(mSwitch);
//        int[] states_check = new int[]{android.R.attr.state_checked};
//        int[] states_default = new int[]{};
//        StateListDrawable trackDrawable = new StateListDrawable();
//        Drawable oriTrackDrawable = getResources().getDrawable(R.drawable.md_track);
//        trackDrawable.setBounds(oriTrackDrawable.getBounds());
//        trackDrawable.addState(states_check,Theme.TintDrawable(oriTrackDrawable,ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.MATERIAL_COLOR_PRIMARY : R.color.purple_782899),0.3f));
//        trackDrawable.addState(states_default,getResources().getDrawable(R.drawable.md_track));
//        mSwitch.setTrackDrawable(trackDrawable);

//        ((LinearLayout)findView(R.id.popup_timer_container)).addView(mSwitch);

//        Theme.TintDrawable(mSwitch.getThumbDrawable(),ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryColorRes() : R.color.purple_782899));
//        Theme.TintDrawable(mSwitch.getTrackDrawable(), ColorUtil.adjustAlpha(ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryColorRes() : R.color.purple_782899),0.7f));

        //读取保存的配置
        boolean hasdefault = SPUtil.getValue(this, "Setting", "TimerDefault", false);
        final int time = SPUtil.getValue(this,"Setting","TimerNum",-1);

        //默认选项
        if(hasdefault && time > 0){
            //如果有默认设置并且没有开始计时，直接开始计时
            //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
            if(!misTiming) {
                mTime = time;
                Toggle();
            }
        }
        mSwitch.setChecked(hasdefault);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mTime > 0) {
                        ToastUtil.show(TimerDialog.this,R.string.set_success);
                        SPUtil.putValue(TimerDialog.this, "Setting", "TimerDefault", true);
                        SPUtil.putValue(TimerDialog.this, "Setting", "TimerNum", (int) mTime);
                    } else {
                        ToastUtil.show(TimerDialog.this,R.string.plz_set_correct_time);
                        mSwitch.setChecked(false);
                    }
                } else {
                    ToastUtil.show(TimerDialog.this,R.string.cancel_success);
                    SPUtil.putValue(TimerDialog.this, "Setting", "TimerDefault", false);
                    SPUtil.putValue(TimerDialog.this, "Setting", "TimerNum", -1);
                }
            }
        });


        mToggle.setText(misTiming ? "取消计时" : "开始计时");
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toggle();
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                misRun = false;
                finish();
            }
        });
    }

    /**
     * 根据是否已经开始计时来取消或开始计时
     */
    private void Toggle(){
        if(mTime <= 0 && !misTiming) {
            ToastUtil.show(TimerDialog.this,R.string.plz_set_correct_time);
            return;
        }
        String msg = misTiming ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
        ToastUtil.show(MainActivity.mInstance,msg);
        misTiming = !misTiming;
        mSeekbar.setStart(misTiming);
        Intent intent = new Intent(Constants.CONTROL_TIMER);
        intent.putExtra("Time", mTime);
        intent.putExtra("Run", misTiming);
        sendBroadcast(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        misRun = true;
        if(misTiming) {
            TimeThread thread = new TimeThread();
            thread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        misRun = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(android.R.anim.fade_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    /**
     * 根据开始计时的时间，每隔一秒重新计算并通过handler更新界面
     */
    class TimeThread extends Thread{
        int min,sec,remain;
        @Override
        public void run(){
            while (misRun){
                remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
                min = remain / 60;
                sec = remain % 60;
                String str_min = min < 10 ? "0" + min : "" + min;
                String str_sec = sec < 10 ? "0" + sec : "" + sec;
                String text = str_min + ":" + str_sec + "min";
                Message msg = new Message();
                msg.obj = text;
                msg.arg1 = min;
                mHandler.sendMessage(msg);
                LogUtil.d("Timer","SendMsg");
                try {
                    sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
