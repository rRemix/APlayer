package remix.myplayer.misc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.LogUtil;

/**
 * Created by taeja on 16-2-5.
 */

/**
 * 接收线控的广播
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private final static String TAG = "MediaButtonReceiver";
    //按下了几次
    private static int mCount = 0;
    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent == null)
            return;
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if(event == null)
            return;
        //过滤按下事件
        boolean isActionUp = (event.getAction()==KeyEvent.ACTION_UP);
        if(!isActionUp) {
            return;
        }
        Intent intent_ctl = null;
        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            LogUtil.d(TAG,"receive remote ctrl");
            intent_ctl = new Intent(MusicService.ACTION_CMD);
            int arg = keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ? Constants.TOGGLE :
                    keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT : Constants.PREV;
            intent_ctl.putExtra("Control", arg);
            context.sendBroadcast(intent_ctl);
            return;
        }

        LogUtil.d(TAG,"count=" + mCount);
        //如果是第一次按下，开启一条线程去判断用户操作
        if(mCount == 0){
            new Thread(){
                @Override
                public void run() {
                    try {
                        sleep(800);
                        int arg = mCount == 1 ? Constants.TOGGLE : mCount == 2 ? Constants.NEXT : Constants.PREV;
                        mCount = 0;
                        Intent intent = new Intent(MusicService.ACTION_CMD);
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
