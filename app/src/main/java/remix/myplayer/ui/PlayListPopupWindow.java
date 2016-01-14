package remix.myplayer.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.adapters.PlayListAdapter;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/6.
 */
public class PlayListPopupWindow extends Activity {
    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_playlist);
        mListView = (ListView)findViewById(R.id.bottom_actionbar_play_list);
        mListView.setAdapter(new PlayListAdapter(getLayoutInflater(), getApplicationContext()));
        mListView.setOnItemClickListener(new ListViewListener());

        //改变播放列表高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (metrics.heightPixels * 0.85);
        lp.width = (int) (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);
    }

    //实现onTouchEvent触屏函数但点击屏幕时销毁本Activity
//    @Override
//    public boolean onTouchEvent(MotionEvent event){
//        finish();
//        return true;
//    }

    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MP3Info temp = (MP3Info) parent.getAdapter().getItem(position);
            Intent intent = new Intent(Utility.CTL_ACTION);
            Bundle arg = new Bundle();
            arg.putInt("Control", Utility.PLAYSELECTEDSONG);
            arg.putInt("Position", position);
            intent.putExtras(arg);
            sendBroadcast(intent);
        }
    }
}
