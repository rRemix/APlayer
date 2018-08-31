package remix.myplayer.misc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import remix.myplayer.Global;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2016/3/23.
 */

/**
 * 接收耳机插入与拔出的广播
 * 当检测到耳机拔出并且正在播放时，发送停止播放的广播
 */
public class HeadsetPlugReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        final String action = intent.getAction();
        boolean headsetOn = true;
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            headsetOn = false;
        }
        if (intent.hasExtra("state")) {
            headsetOn = intent.getIntExtra("state", -1) == 1;
        }

        Global.setHeadsetOn(headsetOn);
        Intent eqintent = new Intent(Constants.SOUNDEFFECT_ACTION);
        eqintent.putExtra("IsHeadsetOn", Global.getHeadsetOn());
        context.sendBroadcast(eqintent);

        if (!headsetOn && MusicServiceRemote.isPlaying()) {
            Intent ctlIntent = new Intent(MusicService.ACTION_CMD);
            ctlIntent.putExtra("Control", Command.PAUSE);
            context.sendBroadcast(ctlIntent);
        }
        try {
            abortBroadcast();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
