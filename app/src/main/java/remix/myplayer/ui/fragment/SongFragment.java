package remix.myplayer.ui.fragment;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import remix.myplayer.R;
import remix.myplayer.adapter.MainSongAdapter;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.interfaces.SortChangeCallback;
import remix.myplayer.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends CursorFragment implements LoaderManager.LoaderCallbacks<List<MP3Item>>{
    @BindView(R.id.recyclerview)
    FastScrollRecyclerView mRecyclerView;
    public static int mSongId = -1;

    private static int LOADER_ID = 10;
    public static final String TAG = SongFragment.class.getSimpleName();
    private MultiChoice mMultiChoice;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_song,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }

        mAdapter = new MainSongAdapter(getActivity(),mCursor,mMultiChoice, SongAdapter.ALLSONG);
        ((SongAdapter)mAdapter).setChangeCallback(new SortChangeCallback() {
            @Override
            public void SortChange() {
                getLoaderManager().restartLoader(LOADER_ID,null,SongFragment.this);
            }

        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int id = getSongID(position);
                if(id > 0 && !mMultiChoice.itemAddorRemoveWithClick(view,position,id,TAG)){
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(Global.AllSongList,getActivity(),intent);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongID(position);
                if(getUserVisibleHint() && id > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG,Constants.SONG);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    /**
     * 重新排序后需要重置全部歌曲列表
     */
    private synchronized void setAllSongList(){
        if(mAdapter == null || ((MainSongAdapter)mAdapter).getData() == null)
            return;
        if(Global.AllSongList == null)
            Global.AllSongList = new ArrayList<>();
        else
            Global.AllSongList.clear();
        for(MP3Item mp3Item : ((MainSongAdapter)mAdapter).getData()){
            Global.AllSongList.add(mp3Item.getId());
        }
    }

    private int getSongID(int position){
        int id = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            id = mCursor.getInt(mSongId);
        }
        return id;
    }

    @Override
    public Loader<List<MP3Item>> onCreateLoader(int id, Bundle args) {
        //查询所有歌曲
        return new AsyncSongLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<List<MP3Item>> loader, final List<MP3Item> data) {
        if(data == null || loader.getId() != LOADER_ID)
            return;
        ((MainSongAdapter)mAdapter).setData(data);
        new Thread(){
            @Override
            public void run() {
                setAllSongList();
            }
        }.start();
    }

    @Override
    public void onLoaderReset(Loader<List<MP3Item>> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAdapter != null){
            mAdapter.setCursor(null);
        }
    }

    @Override
    public SongAdapter getAdapter(){
        return (SongAdapter) mAdapter;
    }

    @Override
    public void onMediaStoreChanged() {
        if(mHasPermission)
            getLoaderManager().initLoader(++LOADER_ID, null, this);
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<MP3Item>> {
        private AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<MP3Item> loadInBackground() {
            return MediaStoreUtil.getAllSong();
        }
    }
}
