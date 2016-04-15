package remix.myplayer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

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
            Intent eqintent = new Intent(Constants.EQENABLE_ACTION);
            eqintent.putExtra("IsHeadsetOn",Global.getHeadsetOn());
            context.sendBroadcast(eqintent);
            if(intent.getIntExtra("state", -1) == 0 && MusicService.getIsplay()){
                Intent intent1 = new Intent(Constants.CTL_ACTION);
                intent1.putExtra("Control",Constants.PAUSE);
                context.sendBroadcast(intent1);
            }
        }
    }
}
