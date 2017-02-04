package remix.myplayer.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.LockScreenActivity;
import remix.myplayer.util.SPUtil;

/**
 * Created by taeja on 16-3-11.
 */
public class LockScreenListener {
    private Context mContext;
    private static LockScreenListener mInstance;
    private static ScreenReceiver mReceiver;
    private LockScreenListener(Context context){
        mContext = context;
        mInstance = this;
    }
    public synchronized static LockScreenListener getInstance(Context context){
        if(mInstance == null){
            mInstance = new LockScreenListener(context);
        }
        return mInstance;
    }
    public void beginListen(){
        mReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mReceiver,filter);
    }
    public void stopListen(){
//        if(mContext != null && mReceiver != null){
//            mContext.unregisterReceiver(mReceiver);
//            mReceiver = null;
//        }
    }
    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!SPUtil.getValue(mContext,"Setting","LockScreenOn",true)){
                return;
            }
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON) && MusicService.isPlay()) {
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
