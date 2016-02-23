package remix.myplayer.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.services.MusicService;
import remix.myplayer.services.NotifyService;

/**
 * Created by taeja on 16-2-16.
 */
public class ExitReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            System.exit(0);
            MusicService.mInstance.stopSelf();
            NotifyService.mInstance.stopSelf();
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            }catch (Exception e){
                e.printStackTrace();
            }

    }
}
