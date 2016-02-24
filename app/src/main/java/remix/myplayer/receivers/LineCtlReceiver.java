package remix.myplayer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RemoteControlClient;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.R;
import remix.myplayer.utils.Constants;

/**
 * Created by taeja on 16-2-5.
 */
public class LineCtlReceiver extends BroadcastReceiver {
    private final static String TAG = "LineCtlReceiver";
    //按下了几次
    private static int mCount = 0;
    //每次按下时间
    private long mSingleTime = 0;
    //连续n次按下总计时间
    private long mTotalTime = 0;
    //两次按下间隔时间
    private long mIntervalTime = 0;
    //上一次按下的时间
    private long mLastTime = 0;
    private Timer mTimer = new Timer();
    @Override
    public void onReceive(final Context context, Intent intent) {
        Intent intent_ctl = null;
        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if(event == null) return;

        //过滤按下事件
        boolean isActionUp = (event.getAction()==KeyEvent.ACTION_UP);
        if(!isActionUp) {
            return;
        }

        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            Log.d(TAG,"receive remote ctrl");
            intent_ctl = new Intent(Constants.CTL_ACTION);
            int arg = keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ? Constants.PLAY :
                    keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT : Constants.PREV;
            intent_ctl.putExtra("Control", arg);
            context.sendBroadcast(intent_ctl);
            return;
        }


        mSingleTime = event.getEventTime() - event.getDownTime();//按键按下到松开的时长
        Log.d(TAG,"count=" + mCount);
        //如果是第一次按下，开启一条线程去判断用户操作
        if(mCount == 0){
            new Thread(){
                @Override
                public void run() {
                    try {
                        sleep(800);
                        int arg = -1;
                        arg = mCount == 1 ? Constants.PLAY : mCount == 2 ? Constants.NEXT : Constants.PREV;
                        mCount = 0;
                        Intent intent = new Intent(Constants.CTL_ACTION);
                        intent.putExtra("Control", arg);
                        context.sendBroadcast(intent);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        mCount++;

        //终止广播(不让别的程序收到此广播，免受干扰)
        abortBroadcast();
    }
}
