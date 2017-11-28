package remix.myplayer.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.LockScreenActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-3-11.
 */
public class LockScreenListener {
    private Context mContext;
    private static LockScreenListener mInstance;
    private ScreenReceiver mReceiver;
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
        CommonUtil.unregisterReceiver(mContext,mReceiver);
    }

    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if(SPUtil.getValue(context,"Setting","LockScreenOn", Constants.APLAYER_LOCKSCREEN) != Constants.APLAYER_LOCKSCREEN){
                    return;
                }
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action) && MusicService.isPlay()) {
                    context.startActivity(new Intent(context, LockScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }catch (Exception e){
                ToastUtil.show(mContext,"LockScreen:" + e.toString());
            }

        }
    }
}
