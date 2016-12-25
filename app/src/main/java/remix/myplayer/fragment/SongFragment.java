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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.helper.DeleteHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 全部歌曲的Fragment
 */
public class SongFragment extends CursorFragment implements LoaderManager.LoaderCallbacks<Cursor>,DeleteHelper.Callback {

    //歌曲名 艺术家 专辑名 专辑id 歌曲id对应的索引
    public static int mDisPlayNameIndex = -1;
    public static int mTitleIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    private static int LOADER_ID = 0;
//    private Cursor mCursor;
//    private SongAdapter mAdapter;

    public static final String TAG = SongFragment.class.getSimpleName();
    private MultiChoice mMultiChoice;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        CURRENT_ID = ++LOADER_ID;
        getLoaderManager().initLoader(CURRENT_ID, null, (LoaderManager.LoaderCallbacks) this);

    }

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

        rootView.findViewById(R.id.play_shuffle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                Global.setPlayQueue(Global.mAllSongList,getContext(),intent);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }
        mAdapter = new SongAdapter(getActivity(),mMultiChoice, SongAdapter.ALLSONG);

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
                    Global.setPlayQueue(Global.mAllSongList,getActivity(),intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongID(position);
                if(getUserVisibleHint() && id > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG,Constants.SONG);
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
                null,MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getDeleteID(),null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {

        if(data == null || loader.getId() != CURRENT_ID)
            return;
        //保存查询结果，并设置查询索引
        mCursor = data;
        try {
            mTitleIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            mDisPlayNameIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            mArtistIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            mAlbumIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            mSongId = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            mAlbumIdIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            mAdapter.setCursor(mCursor);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
    public void OnDelete() {
        CURRENT_ID = ++LOADER_ID;
        getLoaderManager().initLoader(CURRENT_ID, null, this);
    }
}
