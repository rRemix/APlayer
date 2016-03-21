package remix.myplayer.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.umeng.analytics.MobclickAgent;

import remix.myplayer.listeners.LockScreenListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.services.TimerService;
import remix.myplayer.utils.ActivityManager;

/**
 * Created by taeja on 16-2-16.
 */
public class ExitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            MusicService.mInstance.stopSelf();
//            NotifyService.mInstance.stopSelf();
            TimerService.mInstance.stopSelf();
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            if(LockScreenListener.mInstance != null){
                LockScreenListener.mInstance.stopListen();
            }
            MobclickAgent.onKillProcess(context);
            ActivityManager.FinishAll();
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
