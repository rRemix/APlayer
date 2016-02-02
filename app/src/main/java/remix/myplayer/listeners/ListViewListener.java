package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import remix.myplayer.activities.MainActivity;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/6.
 */
public class ListViewListener implements AdapterView.OnItemClickListener
{
    private Context mContext;
    public ListViewListener(Context context)
    {
        mContext = context;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(Utility.CTL_ACTION);
        Bundle arg = new Bundle();
        arg.putInt("Control", Utility.PLAYSELECTEDSONG);
        arg.putInt("Position", position);
        intent.putExtras(arg);
        mContext.sendBroadcast(intent);
        Utility.mPlayingList = Utility.mAllSongList;
//        MainActivity.mInstance.getService().UpdateNextSong(position);

    }
}
