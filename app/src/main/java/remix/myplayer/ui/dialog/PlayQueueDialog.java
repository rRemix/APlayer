package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayQueueAdapter;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.service.MusicService;
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
    Cursor mCursor = null;
    private SeekHandler mHandler;
    public static int mAudioIdIndex;
    private static int LOADER_ID = 0;
    private boolean mMove = false;
    private int mPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playqueue);
        ButterKnife.bind(this);

        mHandler = new SeekHandler(getMainLooper(),this);
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
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //在这里进行第二次滚动（最后的100米！）
                if (mMove){
                    mMove = false;
                    //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
                    int n = mPos - ((LinearLayoutManager)mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    if ( 0 <= n && n < mRecyclerView.getChildCount()){
                        //获取要置顶的项顶部离RecyclerView顶部的距离
                        int top = mRecyclerView.getChildAt(n).getTop();
                        //最后的移动
                        mRecyclerView.scrollBy(0, top);
                    }
                }
            }
        });

        //初始化LoaderManager
        getSupportLoaderManager().initLoader(LOADER_ID++,null,this);

        //改变播放列表高度，并置于底部
        Window w = getWindow();
        w.setWindowAnimations(R.style.AnimBottom);
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
                PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?",new String[]{Global.PlayQueueID + ""},null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        try {
            mCursor = data;
            mAudioIdIndex = data.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID);
            final int curId = MusicService.getCurrentMP3() != null ? MusicService.getCurrentMP3().getId() : -1;
            if(curId < 0)
                return;
            new Thread(){
                @Override
                public void run() {
                    mCursor.moveToFirst();
                    for(int i = 0 ; i < mCursor.getCount();i++){
                        mCursor.moveToPosition(i);
                        if(mCursor.getInt(mAudioIdIndex) == curId){
                            mPos = i;
                            Message msg = mHandler.obtainMessage(0);
                            msg.sendToTarget();
                        }
                    }
                }
            }.start();
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

    private static class SeekHandler extends Handler{
        private final WeakReference<PlayQueueDialog> mRef;

        SeekHandler(Looper looper, PlayQueueDialog dialog) {
            super(looper);
            mRef = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            final PlayQueueDialog dialog = mRef.get();
            final int pos = dialog.mPos;
            final RecyclerView recyclerView = dialog.mRecyclerView;
            final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int firstItem = layoutManager.findFirstVisibleItemPosition();
            int lastItem = layoutManager.findLastVisibleItemPosition();
            //然后区分情况
            if (pos <= firstItem ){
                //当要置顶的项在当前显示的第一个项的前面时
                recyclerView.scrollToPosition(pos);
            }else if (pos <= lastItem ){
                //当要置顶的项已经在屏幕上显示时
                int top = recyclerView.getChildAt(pos - firstItem).getTop();
                recyclerView.scrollBy(0, top);
            }else{
                //当要置顶的项在当前显示的最后一项的后面时
                recyclerView.scrollToPosition(pos);
                //这里这个变量是用在RecyclerView滚动监听里面的
                dialog.mMove = true;
            }
            if(dialog != null && pos >= 0){
                dialog.mRecyclerView.getLayoutManager().scrollToPosition(pos);
            }
        }
    }
}
