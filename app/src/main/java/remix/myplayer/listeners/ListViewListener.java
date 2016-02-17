package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;

import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

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
        Intent intent = new Intent(Constants.CTL_ACTION);
        Bundle arg = new Bundle();
        arg.putInt("Control", Constants.PLAYSELECTEDSONG);
        arg.putInt("Position", position);
        intent.putExtras(arg);
        mContext.sendBroadcast(intent);
        DBUtil.setPlayingList((ArrayList<Long>) DBUtil.mAllSongList.clone());
//        MainActivity.mInstance.getService().UpdateNextSong(position);

    }
}
