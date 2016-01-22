package remix.myplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;

import remix.myplayer.R;
import remix.myplayer.services.TimerService;
import remix.myplayer.utils.Utility;

/**
 * Created by taeja on 16-1-15.
 */
public class TimerPopupWindow extends Activity {
    public static boolean misRun = false;
    private TextView mText;
    private SeekBar mSeekbar;
    private Button mToggle;
    private Button mCancel;
    private static int mTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popup_timer);
        //改变高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (200 * metrics.densityDpi / 160);
        lp.width = (int) (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        mText = (TextView)findViewById(R.id.close_time);
        if(misRun)
        {
            long stoptime = System.currentTimeMillis();
            int runtime = (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000 / 60;
            mText.setText(String.valueOf(mTime - runtime));
        }
        mSeekbar = (SeekBar)findViewById(R.id.close_seekbar);
        mTime = mSeekbar.getProgress();
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress > 0)
                {
                    mText.setText(String.valueOf(progress));
                    mTime = progress;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Intent startIntent = new Intent(TimerPopupWindow.this, TimerService.class);
        startService(startIntent);

        mToggle = (Button)findViewById(R.id.close_toggle);
        mToggle.setText(misRun == true ? "取消计时" : "开始计时");
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = misRun == true ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
                Toast.makeText(TimerPopupWindow.this,msg,Toast.LENGTH_SHORT).show();
                misRun = !misRun;
                Intent intent = new Intent(Utility.CONTROL_TIMER);
                intent.putExtra("Time",mTime);
                intent.putExtra("Run",misRun);
                sendBroadcast(intent);
                finish();
            }
        });
        mCancel = (Button)findViewById(R.id.close_stop);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
