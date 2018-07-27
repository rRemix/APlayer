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
import remix.myplayer.adapter.ArtistAdapter;
import remix.myplayer.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家Fragment
 */
public class ArtistFragment extends LibraryFragment<Artist,ArtistAdapter>{
    @BindView(R.id.artist_recycleview)
    FastScrollRecyclerView mRecyclerView;

    public static final String TAG = ArtistFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_artist;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new ArtistAdapter(mContext,R.layout.item_artist_recycle_grid,mMultiChoice);
        mAdapter.setModeChangeCallback(mode -> {
            mRecyclerView.setLayoutManager(mode == Constants.LIST_MODEL ? new LinearLayoutManager(mContext) : new GridLayoutManager(mContext, 2));
            mRecyclerView.setAdapter(mAdapter);
        });
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int artistId = getArtistId(position);
                if(getUserVisibleHint() && artistId > 0 &&
                        !mMultiChoice.itemClick(position,artistId,TAG)){
                    if (mAdapter.getDatas() != null) {
                        Artist artist =  mAdapter.getDatas().get(position);
                        int artistid = artist.getArtistID();
                        String title = artist.getArtist();
                        Intent intent = new Intent(mContext, ChildHolderActivity.class);
                        intent.putExtra("Id", artistid);
                        intent.putExtra("Title", title);
                        intent.putExtra("Type", Constants.ARTIST);
                        startActivity(intent);
                    }
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                int artistId = getArtistId(position);
                if(getUserVisibleHint() && artistId > 0)
                    mMultiChoice.itemLongClick(position,artistId,TAG,Constants.ARTIST);
            }
        });
    }

    @Override
    protected void initView() {
        int model = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.NAME,"ArtistModel",Constants.GRID_MODEL);
        mRecyclerView.setLayoutManager(model == 1 ? new LinearLayoutManager(mContext) : new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
    }


    private int getArtistId(int position){
        int artistId = -1;
        if(mAdapter.getDatas() != null){
            artistId = mAdapter.getDatas().get(position).getArtistID();
        }
        return artistId;
    }

    @Override
    protected Loader<List<Artist>> getLoader() {
        return new AsyncArtistLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.ARTIST_FRAGMENT;
    }

    @Override
    public ArtistAdapter getAdapter(){
        return mAdapter;
    }


    private static class AsyncArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {
        private AsyncArtistLoader(Context context) {
            super(context);
        }

        @Override
        public List<Artist> loadInBackground() {
            return MediaStoreUtil.getAllArtist();
        }
    }

}
