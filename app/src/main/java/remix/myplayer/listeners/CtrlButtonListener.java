package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/3.
 */
public class CtrlButtonListener implements View.OnClickListener {
    private Context context;
    public CtrlButtonListener(Context context)
    {
        this.context = context;
    }
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Utility.CTL_ACTION);
        switch (v.getId())
        {
            case R.id.playbar_prev:
                intent.putExtra("Control", Utility.PREV);
                break;
            case R.id.playbar_next:
                intent.putExtra("Control", Utility.NEXT);
                break;
            case R.id.playbar_play:
                intent.putExtra("Control", Utility.PLAY);
                break;
        }
        context.sendBroadcast(intent);
    }
}
