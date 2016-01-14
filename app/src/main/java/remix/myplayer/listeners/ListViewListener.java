package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import remix.myplayer.utils.MP3Info;
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
        MP3Info temp = (MP3Info) parent.getAdapter().getItem(position);
        Intent intent = new Intent(Utility.CTL_ACTION);
        Bundle arg = new Bundle();
        arg.putInt("Control", Utility.PLAYSELECTEDSONG);
        arg.putInt("Position", position);
        intent.putExtras(arg);
        mContext.sendBroadcast(intent);
        //将当前选中的歌曲的歌曲名设置为红色
//            TextView title = (TextView)view.findViewById(R.id.displayname);
//            title.setTextColor(Color.RED);
//            //取消上次选中的红色
//            if(mPrev != -1)
//            {
//                TextView prevtitle =  (TextView)parent.getAdapter().getView(mPrev,view,null).findViewById(R.id.displayname);
//                prevtitle.setBackgroundColor(Color.BLACK);
//                //adapter.notifyDataSetInvalidated();
//                System.out.println(prevtitle.getText().toString());
//            }
//            mPrev = position;
    }
}
