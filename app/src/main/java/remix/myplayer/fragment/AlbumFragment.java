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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.AlbumAdater;
import remix.myplayer.adapter.HeaderAdapter;
import remix.myplayer.helper.DeleteHelper;
import remix.myplayer.interfaces.ModeChangeCallback;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑Fragment
 */
public class AlbumFragment extends CursorFragment implements LoaderManager.LoaderCallbacks<Cursor>,DeleteHelper.Callback {
    @BindView(R.id.album_recycleview)
    RecyclerView mRecycleView;

    //专辑名 专辑id 艺术家对应的索引
    public static int mAlbumIdIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mArtistIndex = -1;

    private MultiChoice mMultiChoice;
    private static int LOADER_ID = 0;

    public static final String TAG = AlbumFragment.class.getSimpleName();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getLoaderManager().initLoader(++LOADER_ID, null, (LoaderManager.LoaderCallbacks) this);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_album,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        if(getActivity() instanceof MultiChoiceActivity){
           mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }

        mAdapter = new AlbumAdater(mCursor,getActivity(),mMultiChoice);
        ((AlbumAdater)mAdapter).setModeChangeCallback(new ModeChangeCallback() {
            @Override
            public void OnModeChange(final int mode) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mRecycleView.setLayoutManager(mode == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
                        mRecycleView.setAdapter(mAdapter);
                    }
                });
            }
        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int albumId = getAlbumID(position);
                if(getUserVisibleHint() && albumId > 0 &&
                        !mMultiChoice.itemAddorRemoveWithClick(view,position,albumId,TAG)){
                    if(mCursor != null && mCursor.moveToPosition(position)) {
                        if(mCursor != null && mCursor.moveToPosition(position)) {
                        int albumid = mCursor.getInt(mAlbumIdIndex);
                        String title = mCursor.getString(mAlbumIndex);
                        Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                        intent.putExtra("Id", albumid);
                        intent.putExtra("Title", title);
                        intent.putExtra("Type", Constants.ALBUM);
                        startActivity(intent);
                        }
                    }
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                int albumId = getAlbumID(position);
                if(getUserVisibleHint() && albumId > 0){
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,albumId,TAG,Constants.ALBUM);
                }
            }
        });

        int model = SPUtil.getValue(getActivity(),"Setting","AlbumModel",Constants.GRID_MODEL);
        mRecycleView.setItemAnimator(new DefaultItemAnimator());
        mRecycleView.setLayoutManager(model == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));

        return rootView;
    }

    private int getAlbumID(int position){
        int albumId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            albumId = mCursor.getInt(mAlbumIdIndex);
        }
        return albumId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //根据专辑id 创建Loader
        try {
            return  new CursorLoader(getActivity(),MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"distinct " + MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getDeleteID() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ALBUM_ID,
                    null,
                    MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null || loader.getId() != LOADER_ID)
            return;
        //查询完毕后保存结果，并设置查询索引
        try {
            mCursor = data;
            mAlbumIdIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            mAlbumIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            mArtistIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            mRecycleView.setAdapter(mAdapter);
            mAdapter.setCursor(mCursor);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null){
            mAdapter.setCursor(null);
        }
    }

    @Override
    public AlbumAdater getAdapter(){
        return (AlbumAdater) mAdapter;
    }


    @Override
    public void OnDelete() {
        getLoaderManager().initLoader(++LOADER_ID, null, this);
    }
}
