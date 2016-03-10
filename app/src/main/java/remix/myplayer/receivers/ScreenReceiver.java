package remix.myplayer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import remix.myplayer.activities.LockScreenActivity;
import remix.myplayer.services.MusicService;

/**
 * Created by taeja on 16-3-10.
 */
public class ScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_SCREEN_ON) && MusicService.getIsplay()){
            try {
                Intent intent1 = new Intent(context,LockScreenActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } catch (Exception e){
                e.printStackTrace();
            }

        }

    }
}
