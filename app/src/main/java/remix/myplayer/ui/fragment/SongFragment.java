package remix.myplayer.ui.fragment;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends LibraryFragment<Song,SongAdapter> {
    @BindView(R.id.recyclerview)
    FastScrollRecyclerView mRecyclerView;

    public static final String TAG = SongFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_song;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new SongAdapter(mContext,R.layout.item_song_recycle,mMultiChoice, SongAdapter.ALLSONG,mRecyclerView);
        mAdapter.setChangeCallback(() -> getLoaderManager().restartLoader(getLoaderId(),null,SongFragment.this));
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int id = getSongID(position);

                if(id > 0 && !mMultiChoice.itemAddorRemoveWithClick(view,position,id,TAG)){
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(Global.AllSongList,mContext,intent);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongID(position);
                if(getUserVisibleHint() && id > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG,Constants.SONG);
            }
        });

    }

    @Override
    protected void initView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
    }

    /**
     * 重新排序后需要重置全部歌曲列表
     */
    private synchronized void setAllSongList(){
        if(mAdapter == null || mAdapter.getDatas() == null)
            return;
        if(Global.AllSongList == null)
            Global.AllSongList = new ArrayList<>();
        else
            Global.AllSongList.clear();
        for(Song song : mAdapter.getDatas()){
            Global.AllSongList.add(song.getId());
        }
    }

    private int getSongID(int position){
        int id = -1;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            id = mAdapter.getDatas().get(position).getId();
        }
        return id;
    }

    @Override
    protected Loader<List<Song>> getLoader() {
        return new AsyncSongLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.SONG_FRAGMENT;
    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
        super.onLoadFinished(loader, data);
        new Thread(){
            @Override
            public void run() {
                setAllSongList();
            }
        }.start();
    }

    @Override
    public SongAdapter getAdapter(){
        return mAdapter;
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        private AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return MediaStoreUtil.getAllSong();
        }
    }
}
