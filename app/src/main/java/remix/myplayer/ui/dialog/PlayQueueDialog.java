package remix.myplayer.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager;
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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.PlayListSong;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.adapter.PlayQueueAdapter;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */
public class PlayQueueDialog extends BaseDialogActivity implements LoaderManager.LoaderCallbacks<List<PlayListSong>> {
    @BindView(R.id.bottom_actionbar_play_list)
    RecyclerView mRecyclerView;
    private PlayQueueAdapter mAdapter;
    private static int LOADER_ID = 0;
    private boolean mMove = false;
    private int mPos = -1;

    private MsgHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playqueue);
        ButterKnife.bind(this);

        mHandler = new MsgHandler(this);
        mAdapter = new PlayQueueAdapter(this, R.layout.item_playqueue);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(MusicService.ACTION_CMD);
                Bundle arg = new Bundle();
                arg.putInt("Control", Command.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                Util.sendLocalBroadcast(intent);

                mHandler.postDelayed(() -> mAdapter.notifyDataSetChanged(), 50);
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
                if (mMove) {
                    mMove = false;
                    //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
                    int n = mPos - ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    if (0 <= n && n < mRecyclerView.getChildCount()) {
                        //获取要置顶的项顶部离RecyclerView顶部的距离
                        int top = mRecyclerView.getChildAt(n).getTop();
                        //最后的移动
                        mRecyclerView.scrollBy(0, top);
                    }
                }
            }
        });

        //初始化LoaderManager
        getSupportLoaderManager().initLoader(LOADER_ID++, null, this);
        //改变播放列表高度，并置于底部
        Window w = getWindow();
//        w.setWindowAnimations(R.style.AnimBottom);
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = DensityUtil.dip2px(mContext, 354);
        lp.width = metrics.widthPixels;
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);
    }

    public PlayQueueAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_bottom_in, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }

    //
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.remove();
    }

    @Override
    public Loader<List<PlayListSong>> onCreateLoader(int id, Bundle args) {
        return new AsyncPlayQueueSongLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<List<PlayListSong>> loader, final List<PlayListSong> data) {
        if (data == null)
            return;
        mAdapter.setData(data);
        final int currentId = MusicServiceRemote.getCurrentSong().getId();
        if (currentId < 0)
            return;
        smoothScrollTo(data, currentId);
    }

    /**
     * 滚动到指定位置
     *
     * @param data
     */
    private void smoothScrollTo(List<PlayListSong> data, int currentId) {
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).AudioId == currentId) {
                        mPos = i;
                        Message msg = mHandler.obtainMessage(0);
                        msg.sendToTarget();
                    }
                }
            }
        }.start();
    }

    @Override
    public void onLoaderReset(Loader<List<PlayListSong>> loader) {
        if (mAdapter != null)
            mAdapter.setData(null);
    }

    @Override
    public void onMediaStoreChanged() {
        if (mHasPermission) {
            getSupportLoaderManager().initLoader(LOADER_ID++, null, this);
        } else {
            if (mAdapter != null)
                mAdapter.setData(null);
        }
    }

    @Override
    public void onPermissionChanged(boolean has) {
        onMediaStoreChanged();
    }

    @Override
    public void onPlayListChanged() {
        onMediaStoreChanged();
    }

    private static class AsyncPlayQueueSongLoader extends WrappedAsyncTaskLoader<List<PlayListSong>> {
        private AsyncPlayQueueSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<PlayListSong> loadInBackground() {
            return PlayListUtil.getPlayListSong(Global.PlayQueueID);
        }
    }

    @OnHandleMessage
    public void handleInternal(Message msg) {
        final LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        int lastItem = layoutManager.findLastVisibleItemPosition();
        //然后区分情况
        if (mPos <= firstItem) {
            //当要置顶的项在当前显示的第一个项的前面时
            mRecyclerView.scrollToPosition(mPos);
        } else if (mPos <= lastItem) {
            //当要置顶的项已经在屏幕上显示时
            int top = mRecyclerView.getChildAt(mPos - firstItem).getTop();
            mRecyclerView.scrollBy(0, top);
        } else {
            //当要置顶的项在当前显示的最后一项的后面时
            mRecyclerView.scrollToPosition(mPos);
            //这里这个变量是用在RecyclerView滚动监听里面的
            mMove = true;
        }
        if (mPos >= 0) {
            mRecyclerView.getLayoutManager().scrollToPosition(mPos);
        }
    }
}
