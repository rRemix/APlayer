package remix.myplayer.receivers;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.umeng.analytics.MobclickAgent;

import java.util.List;

import remix.myplayer.activities.MainActivity;
import remix.myplayer.listeners.LockScreenListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.services.NotifyService;
import remix.myplayer.services.TimerService;

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
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
