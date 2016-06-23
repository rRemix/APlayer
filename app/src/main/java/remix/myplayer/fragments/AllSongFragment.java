package remix.myplayer.fragments;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.AllSongAdapter;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

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
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    public static AllSongFragment mInstance = null;
    private RecyclerView mRecyclerView;
    private AllSongAdapter mAdapter;
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

        mAdapter = new AllSongAdapter(mCursor,getActivity());
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

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //查询所有歌曲
        return  new CursorLoader(getActivity(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //保存查询结果，并设置查询索引
        mCursor = data;
        mDisPlayNameIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        mArtistIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        mAlbumIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        mSongId = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        mAlbumIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        if(mCursor != null) {
            mAdapter.setCursor(mCursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }

    public AllSongAdapter getAdapter(){
        return mAdapter;
    }
}
