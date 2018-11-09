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
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.adapter.SongAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends LibraryFragment<Song, SongAdapter> {
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
        mAdapter = new SongAdapter(mContext, R.layout.item_song_recycle, mChoice, SongAdapter.ALLSONG, mRecyclerView);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final Song song = mAdapter.getDatas().get(position);
                if (getUserVisibleHint() && !mChoice.click(position, song)) {
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Command.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    MusicServiceRemote.setAllSongAsPlayQueue(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (getUserVisibleHint())
                    mChoice.longClick(position, mAdapter.getDatas().get(position));
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
    private synchronized void setAllSongList() {
        if (mAdapter == null || mAdapter.getDatas() == null)
            return;
        List<Integer> allSong = new ArrayList<>();
        for (Song song : mAdapter.getDatas()) {
            allSong.add(song.getId());
        }
        MusicServiceRemote.setAllSong(allSong);
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
        new Thread() {
            @Override
            public void run() {
                setAllSongList();
            }
        }.start();
    }

    @Override
    public SongAdapter getAdapter() {
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
