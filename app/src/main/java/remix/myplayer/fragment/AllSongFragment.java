package remix.myplayer.fragment;


import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapter.AllSongAdapter;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.TabTextListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.ui.customview.IndexView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.sort.Compator;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class AllSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private LoaderManager mManager;
    private Cursor mCursor = null;
    //歌曲名 艺术家 专辑名 专辑id 歌曲id对应的索引
    public static int mDisPlayNameIndex = -1;
    public static int mTitleIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    public static AllSongFragment mInstance = null;
    private RecyclerView mRecyclerView;
    private AllSongAdapter mAdapter;
    private IndexView mIndexView;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mAdapter.setSortList((ArrayList<String>) msg.obj);
            mAdapter.setCursor(mCursor);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mManager = getLoaderManager();
        mManager.initLoader(1000, null, this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCursor != null) {
            mCursor.close();
        }
        if(mAdapter != null){
            mAdapter.setCursor(null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_allsong,null);
        rootView.findViewById(R.id.play_shuffle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                Global.setPlayingList(Global.mAllSongList);
                getActivity().sendBroadcast(intent);
            }
        });
        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(getContext(),RecyclerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new AllSongAdapter(getActivity(),AllSongAdapter.ALLSONG);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Global.setPlayingList(Global.mAllSongList);
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                getActivity().sendBroadcast(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mIndexView = (IndexView)rootView.findViewById(R.id.song_index_view);
        mIndexView.setVisibility(Global.mIndexOpen ? View.VISIBLE : View.GONE);
        mIndexView.setPositionChangedListener(new IndexView.OnLetterChangedListener() {
            @Override
            public void onLetterChanged(char c) {
                if(mAdapter != null && mRecyclerView != null){
                    int pos = mAdapter.getPositionForSection(c);
                    int i = 0;
                    for(; i < 26 ;i++){
                        int posforsec = mAdapter.getPositionForSection('A' + i);
                        if(posforsec >= pos)
                            break;
                    }

                    int dy = pos * DensityUtil.dip2px(getContext(),72) /*+ i * DensityUtil.dip2px(getContext(),20)*/;
                    int scrolly = getScollYDistance();
                    mRecyclerView.scrollBy(0,dy - scrolly);
                    Toast.makeText(getActivity(),String.valueOf(c),Toast.LENGTH_SHORT).show();
                }
            }
        });
        return rootView;
    }

    public int getScollYDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisiableChildView = layoutManager.findViewByPosition(position);
        int itemHeight = firstVisiableChildView.getHeight();
        return (position) * itemHeight - firstVisiableChildView.getTop();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //查询所有歌曲
        return  new CursorLoader(getActivity(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        //保存查询结果，并设置查询索引
        mCursor = data;


        mTitleIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        mDisPlayNameIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        mArtistIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        mAlbumIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        mSongId = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        mAlbumIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

        new IndexThread().start();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }

    public AllSongAdapter getAdapter(){
        return mAdapter;
    }

    class IndexThread extends Thread{
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            ArrayList<String> list = new ArrayList<String>();
            if(Global.mIndexOpen){
                while (mCursor.moveToNext()) {
                    String firstLetter = Compator.getFirstLetter(mCursor.getString(mTitleIndex));
                    Log.d("AllSongFragment","\nTitle:" + mCursor.getString(mTitleIndex) + " \nFirstLetter:" + firstLetter);
                    list.add(firstLetter);
                }
            }
            Message msg = new Message();
            msg.obj = list;
            mHandler.sendMessage(msg);

            Log.d("ALlSongFragment","CostTime:" + (System.currentTimeMillis() - start));
        }
    }

}
