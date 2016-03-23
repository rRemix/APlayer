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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.services.TimerService;
import remix.myplayer.ui.customviews.CircleSeekBar;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class TimerDialog extends BaseActivity {
    //正在计时
    public static boolean misTiming = false;
    //正在运行
    public static boolean misRun = false;
    private TextView mText;
    private CircleSeekBar mSeekbar;
    private TextView mToggle;
    private TextView mCancel;
    private SwitchCompat mSwitch;
    private static long mTime;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mText.setText(msg.obj.toString());
            mSeekbar.setProgress(msg.arg1);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popup_timer);
        //居中显示
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        lp.height = (int) (metrics.heightPixels * 0.7);
//        lp.width = (int) (metrics.widthPixels * 0.73);
//        lp.height = (int) ();
//        lp.width = (int) (metrics.widthPixels * 0.73);

        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);

        mText = (TextView)findViewById(R.id.close_time);
        mSeekbar = (CircleSeekBar) findViewById(R.id.close_seekbar);
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
        mSwitch = (SwitchCompat)findViewById(R.id.popup_timer_switch);

        if(hasdefault && time > 0){
            //如果有默认设置并且没有开始计时，直接开始计时
            //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
            if(!misTiming) {
                mTime = time;
                Toggle();
            } else {
                mSwitch.setThumbResource(R.drawable.timer_btn_seleted_btn);
                mSwitch.setTrackResource(R.drawable.timer_btn_seleted_locus);
            }
        }
        mSwitch.setChecked(hasdefault);

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mTime > 0) {
                        Toast.makeText(TimerDialog.this, "设置成功", Toast.LENGTH_SHORT).show();
                        mSwitch.setThumbResource(R.drawable.timer_btn_seleted_btn);
                        mSwitch.setTrackResource(R.drawable.timer_btn_seleted_locus);
                        SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerDefault", true);
                        SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerNum", (int) mTime);
                    } else {
                        Toast.makeText(TimerDialog.this, "请设置正确的时间", Toast.LENGTH_SHORT).show();
                        mSwitch.setChecked(false);
                    }
                } else {
                    Toast.makeText(TimerDialog.this, "取消成功", Toast.LENGTH_SHORT).show();
                    mSwitch.setThumbResource(R.drawable.timer_btn_normal_btn);
                    mSwitch.setTrackResource(R.drawable.timer_btn_normal_locus);
                    SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerDefault", false);
                    SharedPrefsUtil.putValue(TimerDialog.this, "setting", "TimerNum", -1);
                }
            }
        });


        mToggle = (TextView)findViewById(R.id.close_toggle);
        mToggle.setText(misTiming == true ? "取消计时" : "开始计时");
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toggle();
            }
        });
        mCancel = (TextView)findViewById(R.id.close_stop);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                misRun = false;
                finish();
            }
        });
    }

    private void Toggle(){
        if(mTime <= 0 && !misTiming) {
            Toast.makeText(TimerDialog.this, "请设置正确的时间", Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = misTiming == true ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
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
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.popup_out);
    }

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
