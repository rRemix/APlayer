package remix.myplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.util.Constants;

/**
 * Created by taeja on 16-4-18.
 */
public class EQReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Constants.SOUNDEFFECT_ACTION)){
            boolean isheadsetOn = intent.getExtras().getBoolean("IsHeadsetOn");
            boolean enable = isheadsetOn & EQActivity.getInitialEnable();
            EQActivity.setEnable(enable);
            if(EQActivity.mInstance != null){
                EQActivity.mInstance.UpdateEnable(enable);
            }

        }
    }
}
