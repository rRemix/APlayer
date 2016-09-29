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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.AlbumAdater;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑Fragment
 */
public class AlbumFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @BindView(R.id.album_recycleview)
    RecyclerView mRecycleView;
    Cursor mCursor = null;
    //专辑名 专辑id 艺术家对应的索引
    public static int mAlbumIdIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mArtistIndex = -1;
    private AlbumAdater mAdapter;
    private static int LOADER_ID = 1;
    private MultiChoice mMultiChoice;

    public static final String TAG = AlbumFragment.class.getSimpleName();
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //初始化LoaderManager
        LoaderManager manager = getLoaderManager();
        manager.initLoader(LOADER_ID++, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_album,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        mRecycleView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
//        mRecycleView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
        mRecycleView.setItemAnimator(new DefaultItemAnimator());

        if(getActivity() instanceof MultiChoiceActivity){
           mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }
        mAdapter = new AlbumAdater(mCursor,getActivity(),mMultiChoice);
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
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,albumId,TAG);
                }
            }
        });
        mRecycleView.setAdapter(mAdapter);
        return rootView;
    }

    private int getAlbumID(int position){
        int albumId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            albumId = mCursor.getInt(AlbumFragment.mAlbumIdIndex);
        }
        return albumId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //根据专辑id 创建Loader
//        CursorLoader loader = new CursorLoader(getActivity(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                new String[]{BaseColumns._ID,MediaStore.Audio.AlbumColumns.ALBUM,
//                        MediaStore.Audio.AlbumColumns.ARTIST,
//                        MediaStore.Audio.AlbumColumns.ALBUM_ART,
//                        MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS}, null,null,null);
        return  new CursorLoader(getActivity(),MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{"distinct " + MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.ARTIST},
                MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + ")" + " GROUP BY (" + MediaStore.Audio.Media.ALBUM_ID,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        mCursor = data;
        mAlbumIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        mAlbumIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        mArtistIndex = data.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        mAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public AlbumAdater getAdapter(){
        return mAdapter;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCursor != null)
            mCursor.close();
    }

}
