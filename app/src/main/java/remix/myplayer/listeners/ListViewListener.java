package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;

import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 播放选中歌曲
 */

public class ListViewListener implements AdapterView.OnItemClickListener {
    private Context mContext;
    public ListViewListener(Context context) {
        mContext = context;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Global.setPlayingList((ArrayList<Long>) Global.mAllSongList.clone());
        Intent intent = new Intent(Constants.CTL_ACTION);
        Bundle arg = new Bundle();
        arg.putInt("Control", Constants.PLAYSELECTEDSONG);
        arg.putInt("Position", position);
        intent.putExtras(arg);
        mContext.sendBroadcast(intent);

        view.setSelected(true);
//        mName.setTextColor(Color.parseColor("#ff0030"));
        //选中item的歌曲名变红
//        MainActivity.mInstance.getService().UpdateNextSong(position);

    }
}
