package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayQueueAdapter;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */
public class PlayQueueDialog extends BaseDialogActivity implements LoaderManager.LoaderCallbacks<Cursor>  {
    @BindView(R.id.bottom_actionbar_play_list)
    RecyclerView mRecyclerView;
    private PlayQueueAdapter mAdapter;
    public static PlayQueueDialog mInstance;
    Cursor mCursor = null;
    public static int mAudioIdIndex;
    private static int LOADER_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playqueue);
        ButterKnife.bind(this);

        mInstance = this;
        mAdapter = new PlayQueueAdapter(this);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                sendBroadcast(intent);
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        //初始化LoaderManager
        getSupportLoaderManager().initLoader(LOADER_ID++,null,this);

        //改变播放列表高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (metrics.heightPixels * 0.55);
        lp.width = metrics.widthPixels;
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);
    }

    public PlayQueueAdapter getAdapter(){
        return mAdapter;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_bottom_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null)
            mCursor.close();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,PlayListSongs.CONTENT_URI,new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
                PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?",new String[]{Global.mPlayQueueID + ""},null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        try {
            mCursor = data;
            mAudioIdIndex = data.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID);
            mAdapter.setCursor(data);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }
}
