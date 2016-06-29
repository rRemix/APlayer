package remix.myplayer.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import remix.myplayer.ui.activity.LockScreenActivity;
import remix.myplayer.service.MusicService;

/**
 * Created by taeja on 16-3-11.
 */
public class LockScreenListener {
    private Context mContext;
    public static LockScreenListener mInstance;
    private ScreenReceiver mReceiver;
    public LockScreenListener(Context context){
        mContext = context;
        mInstance = this;
    }
    public void beginListen(){
        mReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mReceiver,filter);
    }
    public void stopListen(){
        mContext.unregisterReceiver(mReceiver);
        mReceiver = null;
    }
    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON) && MusicService.getIsplay()) {
                try {
                    Intent intent1 = new Intent(context, LockScreenActivity.class);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
