package remix.myplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by taeja on 16-1-15.
 */
public class TimerService extends Service {
    public static boolean mFlag = false;
    private int mTime;
    private Timer mTimer = null;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    class TimerReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTime = intent.getIntExtra("Time",-1);
            mFlag = intent.getBooleanExtra("Flag",false);
            //关闭定时
            if(!mFlag)
            {
                mTimer.cancel();
                mTimer = null;
            }
            else
            {
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            wait(mTime * 60 * 1000);
                            System.exit(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },0);
            }
        }
    }
}
