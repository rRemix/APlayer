package remix.myplayer.ui.popupwindow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.adapters.PlayingListAdapter;
import remix.myplayer.utils.Constants;

/**
 * Created by Remix on 2015/12/6.
 */
public class PlayingListPopupWindow extends BaseActivity {
    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_playinglist);
        mListView = (ListView)findViewById(R.id.bottom_actionbar_play_list);
        mListView.setAdapter(new PlayingListAdapter(getLayoutInflater(), getApplicationContext()));
        mListView.setOnItemClickListener(new ListViewListener());

        //改变播放列表高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (metrics.heightPixels * 0.55);
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

    private class ListViewListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(Constants.CTL_ACTION);
            Bundle arg = new Bundle();
            arg.putInt("Control", Constants.PLAYSELECTEDSONG);
            arg.putInt("Position", position);
            intent.putExtras(arg);
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
