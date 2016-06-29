package remix.myplayer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.util.Constants;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭Service
 */
public class TimerService extends BaseService {
    /**
     * 是否正在计时
     */
    public static boolean mRun = false;

    /**
     * 定时时长
     */
    private long mTime;

    /**
     * Timer
     */
    private Timer mTimer = null;

    /**
     * 接收计时或者取消的Receiver
     */
    private TimerReceiver mReceiver;

    /**
     * 定时开始的时间
     */
    public static long mStartTime;

    public static TimerService mInstance;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //添加到servicemanager
        mInstance = this;
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


    /**
     * 根据收到广播的参数
     * 开始或者停止定时关闭
     */
    class TimerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTime = intent.getLongExtra("Time",-1);
            mRun = intent.getBooleanExtra("Run",false);
            //关闭定时
            if(!mRun) {
                if(mTimer != null){
                    mTimer.cancel();
                    mTimer = null;
                }
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
                            //记录下开始定时的时间
                            mStartTime = System.currentTimeMillis();
                            Thread.sleep(mTime * 60 * 1000);
                            //时间到后发送关闭程序的广播
                            sendBroadcast(new Intent(Constants.EXIT));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },0);
            }
        }
    }
}
