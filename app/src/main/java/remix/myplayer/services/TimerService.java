package remix.myplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.utils.Constants;

/**
 * Created by taeja on 16-1-15.
 */
public class TimerService extends Service {
    public static boolean mRun = false;
    private long mTime;
    private Timer mTimer = null;
    private TimerReceiver mReceiver;
    public static long mStartTime;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new TimerReceiver();
        IntentFilter filter = new IntentFilter(Constants.CONTROL_TIMER);
        registerReceiver(mReceiver,filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    class TimerReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTime = intent.getLongExtra("Time",-1);
            mRun = intent.getBooleanExtra("Run",false);
            //关闭定时
            if(!mRun) {
                mTimer.cancel();
                mTimer = null;
                mStartTime = -1;
            }
            else {
                if(mTime < 0)
                    return;
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            mStartTime= System.currentTimeMillis();
                            Thread.sleep(mTime * 60 * 1000);
                            System.exit(0);
//                            sendBroadcast(new Intent(CommonUtil.EXIT));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },0);
            }
        }
    }
}
