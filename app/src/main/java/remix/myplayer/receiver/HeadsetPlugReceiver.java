package remix.myplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

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
        if(intent.hasExtra("state")){
            Global.setHeadsetOn(intent.getIntExtra("state", -1) == 1);
            Intent eqintent = new Intent(Constants.SOUNDEFFECT_ACTION);
            eqintent.putExtra("IsHeadsetOn",Global.getHeadsetOn());
            context.sendBroadcast(eqintent);
            if(intent.getIntExtra("state", -1) == 0 && MusicService.isPlay()){
                Intent intent1 = new Intent(Constants.CTL_ACTION);
                intent1.putExtra("Control",Constants.PAUSE);
                context.sendBroadcast(intent1);
            }
        }
    }
}
