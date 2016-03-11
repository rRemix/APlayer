package remix.myplayer.receivers;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
            ActivityManager activityManger = (ActivityManager) MainActivity.mInstance.getSystemService(Context.ACTIVITY_SERVICE);// 获取Activity管理器
            List<ActivityManager.RunningServiceInfo> serviceList = activityManger.getRunningServices(30);// 从窗口管理器中获取正在运行的Service
            List<ActivityManager.AppTask> applist = activityManger.getAppTasks();

            for(ActivityManager.RunningServiceInfo info : serviceList){
                System.out.println("classname:" + info.service.getClassName());
                System.out.println("packagename" + info.service.getPackageName());
            }

            MusicService.mInstance.stopSelf();
            NotifyService.mInstance.stopSelf();
            TimerService.mInstance.stopSelf();
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            if(LockScreenListener.mInstance != null){
                LockScreenListener.mInstance.stopListen();
            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
