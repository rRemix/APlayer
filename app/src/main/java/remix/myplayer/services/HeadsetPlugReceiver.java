package remix.myplayer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.utils.Constants;

/**
 * Created by Remix on 2016/3/23.
 */
public class HeadsetPlugReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra("state")){
            if(intent.getIntExtra("state", -1) == 0 && MusicService.getIsplay()){
                Intent intent1 = new Intent(Constants.CTL_ACTION);
                intent1.putExtra("Control",Constants.PAUSE);
                context.sendBroadcast(intent1);
            }
        }
    }
}
