package remix.myplayer.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import java.util.List;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.AlbumAdater;
import remix.myplayer.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.ModeChangeCallback;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑Fragment
 */
public class AlbumFragment extends LibraryFragment<Album,AlbumAdater>{
    @BindView(R.id.album_recycleview)
    FastScrollRecyclerView mRecyclerView;

    public static final String TAG = AlbumFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_album;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new AlbumAdater(mContext,R.layout.item_album_recycle_grid,mMultiChoice);
        mAdapter.setModeChangeCallback(new ModeChangeCallback() {
            @Override
            public void OnModeChange(final int mode) {
                mRecyclerView.setLayoutManager(mode == Constants.LIST_MODEL ? new LinearLayoutManager(mContext) : new GridLayoutManager(getActivity(), 2));
                mRecyclerView.setAdapter(mAdapter);
            }
        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int albumId = getAlbumID(position);
                if(getUserVisibleHint() && albumId > 0 &&
                        !mMultiChoice.itemAddorRemoveWithClick(view,position,albumId,TAG)){
                    if(mAdapter.getDatas() != null){
                        Album album = mAdapter.getDatas().get(position);
                        int albumid = album.getAlbumID();
                        String title = album.getAlbum();
                        Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                        intent.putExtra("Id", albumid);
                        intent.putExtra("Title", title);
                        intent.putExtra("Type", Constants.ALBUM);
                        startActivity(intent);
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
    }

    @Override
    protected void initView() {
        int model = SPUtil.getValue(getActivity(),"Setting","AlbumModel",Constants.GRID_MODEL);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(model == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setAdapter(mAdapter);
    }

    private int getAlbumID(int position){
        int albumId = -1;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            albumId = mAdapter.getDatas().get(position).getAlbumID();
        }
        return albumId;
    }


    @Override
    public AlbumAdater getAdapter(){
        return mAdapter;
    }


    @Override
    protected Loader<List<Album>> getLoader() {
        return new AsyncAlbumLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.ALBUM_FRAGMENT;
    }

    private static class AsyncAlbumLoader extends WrappedAsyncTaskLoader<List<Album>> {
        private AsyncAlbumLoader(Context context) {
            super(context);
        }

        @Override
        public List<Album> loadInBackground() {
            return MediaStoreUtil.getALlAlbum();
        }
    }
}
