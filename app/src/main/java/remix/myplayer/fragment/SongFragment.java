package remix.myplayer.fragment;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private LoaderManager mManager;
    private Cursor mCursor = null;
    //歌曲名 艺术家 专辑名 专辑id 歌曲id对应的索引
    public static int mDisPlayNameIndex = -1;
    public static int mTitleIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    public static SongFragment mInstance = null;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;
    private SongAdapter mAdapter;
    private static int LOADER_ID = 1;
    public static boolean isFirstSelected = true;
    public static final String TAG = SongFragment.class.getSimpleName();


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mManager = getLoaderManager();
        mManager.initLoader(LOADER_ID++, null, this);
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
        final View rootView = inflater.inflate(R.layout.fragment_song,null);
        mUnBinder = ButterKnife.bind(this,rootView);
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

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(getContext(),RecyclerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new SongAdapter(getActivity(), SongAdapter.ALLSONG);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int id = getSongID(position);
                if(id > 0 && !MainActivity.MultiChoice.itemAddorRemoveWithClick(view,position,id,TAG)){
                    Global.setPlayingList(Global.mAllSongList);
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    getActivity().sendBroadcast(intent);
                }

            }

            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongID(position);
                if(getUserVisibleHint() && id > 0)
                    MainActivity.MultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG);

            }
        });
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    private int getSongID(int position){
        int id = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            id = mCursor.getInt(mSongId);
        }
        return id;
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
        mAdapter.setCursor(mCursor);
//        new IndexThread().start();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public SongAdapter getAdapter(){
        return mAdapter;
    }


//    class IndexThread extends Thread{
//        @Override
//        public void run() {
//            long start = System.currentTimeMillis();
//            ArrayList<String> list = new ArrayList<String>();
//            if(Global.mIndexOpen){
//                while (mCursor.moveToNext()) {
//                    String firstLetter = Compator.getFirstLetter(mCursor.getString(mTitleIndex));
//                    Log.d("SongFragment","\nTitle:" + mCursor.getString(mTitleIndex) + " \nFirstLetter:" + firstLetter);
//                    list.add(firstLetter);
//                }
//            }
//            Message msg = new Message();
//            msg.obj = list;
//            mHandler.sendMessage(msg);
//
//            Log.d("ALlSongFragment","CostTime:" + (System.currentTimeMillis() - start));
//        }
//    }

}
