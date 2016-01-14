package remix.myplayer.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.utils.MP3Info;

/**
 * Created by Remix on 2015/12/2.
 */
public class receiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        MP3Info temp = (MP3Info)intent.getExtras().getSerializable("MP3Info");
        if(temp != null)
        {

        }
    }
}
