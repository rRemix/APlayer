package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import remix.myplayer.utils.Constants;

/**
 * Created by Remix on 2015/12/10.
 */
public class SlideMenuListener implements AdapterView.OnItemClickListener {
    private Context mContext;
    public SlideMenuListener(Context mContext) {
        this.mContext = mContext;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (view.getId()) {
            case 0:
                Toast.makeText(mContext, "全部歌曲", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(mContext, "播放列表", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.PREV);
                mContext.sendBroadcast(intent);
                break;
            default:break;
        }
    }
}
