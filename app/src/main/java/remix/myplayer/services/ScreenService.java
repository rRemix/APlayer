package remix.myplayer.services;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import remix.myplayer.activities.LockScreenActivity;

/**
 * Created by Remix on 2016/3/9.
 */
public class ScreenService extends Service {
    private final static String TAG = "ScreenService";
    private ScreenReceiver mReceiver;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    // 键盘管理器
    KeyguardManager mKeyguardManager;
    // 键盘锁
    private KeyguardManager.KeyguardLock mKeyguardLock;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        //获取电源服务
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if(mWakeLock != null){
            mWakeLock.release();
            mWakeLock = null;
        }
        if (mKeyguardLock!=null) {
            mKeyguardLock.reenableKeyguard();
        }
    }

    class ScreenReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"intent:" + intent.getAction());
            if(action.equals(Intent.ACTION_SCREEN_ON)){
//                try {
//                    Intent intent1 = new Intent(context,LockScreenActivity.class);
//                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent1);
//                } catch (Exception e){
//                    e.printStackTrace();
//                }

//                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"Tag");
//                mWakeLock.acquire();
//                // 初始化键盘锁
//                mKeyguardLock = mKeyguardManager.newKeyguardLock("");
//                // 键盘解锁
//                mKeyguardLock.disableKeyguard();
            }

        }
    }
}
