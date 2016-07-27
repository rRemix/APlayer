package remix.myplayer.ui.dialog;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.ui.activity.BaseActivity;
import remix.myplayer.adapter.PlayingListAdapter;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */
public class PlayingListDialog extends BaseActivity {
    @BindView(R.id.bottom_actionbar_play_list)
    ListView mListView;
    private PlayingListAdapter mAdapter;
    public static PlayingListDialog mInstance;
    private static boolean mNeedRefresh = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_playinglist);
        ButterKnife.bind(this);

        mInstance = this;
        mAdapter = new PlayingListAdapter( getApplicationContext());
        mListView.setAdapter(mAdapter);
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

    public PlayingListAdapter getAdapter(){
        return mAdapter;
    }

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

    public void UpdateAdapter() {
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }

    public static void setFresh(boolean needfresh){
        mNeedRefresh = true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
//        if(mNeedRefresh){
//            UpdateAdapter();
//            mNeedRefresh = false;
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
