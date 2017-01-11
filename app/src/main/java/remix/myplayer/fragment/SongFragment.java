package remix.myplayer.fragment;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.gsm.GsmCellLocation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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
import remix.myplayer.util.SPUtil;

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
    @BindView(R.id.sort)
    TextView mSort;
    @BindView(R.id.asc_desc)
    TextView mAscDesc;
    @BindView(R.id.shuffle_container)
    View mShuffle;
    private static int LOADER_ID = 0;
    public static final String TAG = SongFragment.class.getSimpleName();
    private MultiChoice mMultiChoice;
    public static String ASCDESC = " asc";
    public static String SORT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //读取排序方式
        ASCDESC = SPUtil.getValue(context,"Setting","AscDesc"," asc");
        SORT = SPUtil.getValue(context,"Setting","Sort",MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        getLoaderManager().initLoader(++LOADER_ID, null, (LoaderManager.LoaderCallbacks) this);

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
        mRecyclerView.setAdapter(mAdapter);
        //显示当前排序方式
        mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? "按字母" : "按添加时间");
        mAscDesc.setText(!ASCDESC.equals(" desc") ? "升序" : "降序");
        return rootView;
    }


    @OnClick({R.id.asc_desc,R.id.sort,R.id.play_shuffle})
    public void OnClick(View v){
        switch (v.getId()){
            case R.id.play_shuffle:
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                Global.setPlayQueue(Global.AllSongList,getContext(),intent);
                break;
            case R.id.asc_desc:
                if(ASCDESC.equals(" asc")){
                    ASCDESC = " desc";
                } else {
                    ASCDESC = " asc";
                }
                mAscDesc.setText(ASCDESC.equals(" desc") ? "升序" : "降序");
                getLoaderManager().restartLoader(LOADER_ID,null,this);
                SPUtil.putValue(mContext,"Setting","AscDesc", ASCDESC);
                break;
            case R.id.sort:
                if(SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER)){
                    SORT = MediaStore.Audio.Media.DATE_ADDED;
                } else {
                    SORT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
                }
                mSort.setText(SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? "按字母" : "按添加时间");
                getLoaderManager().restartLoader(LOADER_ID,null,this);
                SPUtil.putValue(mContext,"Setting","Sort",SORT);
                break;
        }
    }

    /**
     * 重新排序后需要重置全部歌曲列表
     */
    private synchronized void setAllSongList(){
        if(mCursor == null)
            return;
        if(Global.AllSongList == null)
            Global.AllSongList = new ArrayList<>();
        else
            Global.AllSongList.clear();
        for(int i = 0 ; i < mCursor.getCount();i++){
            if(mCursor.moveToPosition(i)){
                Global.AllSongList.add(mCursor.getInt(mSongId));
            }
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //查询所有歌曲
        return  new CursorLoader(getActivity(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getDeleteID(),
                null,
                SORT + ASCDESC);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        if(data == null || loader.getId() != LOADER_ID)
            return;
        //保存查询结果，并设置查询索引
        mCursor = data;
        try {
            mShuffle.setVisibility(mCursor.getCount() > 0 ? View.VISIBLE : View.GONE);
            mTitleIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            mDisPlayNameIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            mArtistIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            mAlbumIndex = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            mSongId = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            mAlbumIdIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            mAdapter.setCursor(mCursor);
            setAllSongList();
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
        getLoaderManager().initLoader(++LOADER_ID, null, this);
    }
}
