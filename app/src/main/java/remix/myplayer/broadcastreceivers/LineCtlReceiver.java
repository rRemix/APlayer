package remix.myplayer.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Created by taeja on 16-2-5.
 */
public class LineCtlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        Toast.makeText(context,"线控操作",Toast.LENGTH_SHORT).show();
        abortBroadcast();
    }
}
