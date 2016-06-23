package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.ui.activities.BaseActivity;
import remix.myplayer.ui.activities.MainActivity;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.services.TimerService;
import remix.myplayer.ui.customviews.CircleSeekBar;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseActivity {

    //剩余时间
    @ViewInject(R.id.close_time)
    private TextView mText;
    //设置或取下默认
    @ViewInject(R.id.popup_timer_switch)
    private SwitchCompat mSwitch;
    //圆形seekbar
    @ViewInject(R.id.close_seekbar)
    private CircleSeekBar mSeekbar;
    //开始或取消计时
    @ViewInject(R.id.close_toggle)
    private TextView mToggle;
    @ViewInject(R.id.close_stop)
    private TextView mCancel;

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
    public int getLayoutId() {
        return R.layout.popup_timer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        boolean hasdefault = SharedPrefsUtil.getValue(this, "setting", "TimerDefault", false);
        final int time = SharedPrefsUtil.getValue(this,"setting","TimerNum",-1);

        //默认选项
        if(hasdefault && time > 0){
            //如果有默认设置并且没有开始计时，直接开始计时
            //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
            if(!misTiming) {
                mTime = time;
                Toggle();
            } else {
                mSwitch.setThumbResource(R.drawable.timer_btn_seleted_btn);
                mSwitch.setTrackResource(R.drawable.timer_btn_seleted_focus);
            }
        }
        mSwitch.setChecked(hasdefault);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mTime > 0) {
                        Toast.makeText(TimerDialog.this, getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                        mSwitch.setThumbResource(R.drawable.timer_btn_seleted_btn);
                        mSwitch.setTrackResource(R.drawable.timer_btn_seleted_focus);
                        SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerDefault", true);
                        SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerNum", (int) mTime);
                    } else {
                        Toast.makeText(TimerDialog.this, getString(R.string.plz_set_correct_time), Toast.LENGTH_SHORT).show();
                        mSwitch.setChecked(false);
                    }
                } else {
                    Toast.makeText(TimerDialog.this, getString(R.string.cancel_success), Toast.LENGTH_SHORT).show();
                    mSwitch.setThumbResource(R.drawable.timer_btn_normal_btn);
                    mSwitch.setTrackResource(R.drawable.timer_btn_normal_focus);
                    SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerDefault", false);
                    SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerNum", -1);
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
            Toast.makeText(TimerDialog.this, getString(R.string.plz_set_correct_time), Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = misTiming ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
        Toast.makeText(MainActivity.mInstance,msg,Toast.LENGTH_SHORT).show();
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
        overridePendingTransition(R.anim.popup_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.popup_out);
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
                Log.d("Timer","SendMsg");
                try {
                    sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
