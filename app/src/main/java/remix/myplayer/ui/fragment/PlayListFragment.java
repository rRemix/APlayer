package remix.myplayer.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;

import java.util.List;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class PlayListFragment extends LibraryFragment<PlayList,PlayListAdapter>{
    public static final String TAG = PlayListFragment.class.getSimpleName();
    @BindView(R.id.playlist_recycleview)
    FastScrollRecyclerView mRecyclerView;

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_playlist;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new PlayListAdapter(mContext,R.layout.item_playlist_recycle_grid,mMultiChoice);
        mAdapter.setModeChangeCallback(mode -> {
            mRecyclerView.setLayoutManager(mode == Constants.LIST_MODEL ? new LinearLayoutManager(mContext) : new GridLayoutManager(getActivity(), 2));
            mRecyclerView.setAdapter(mAdapter);
        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = getPlayListName(position);
                if(!TextUtils.isEmpty(name) && !mMultiChoice.itemAddorRemoveWithClick(view,position,getPlayListId(position),TAG)){
                    if(getPlayListSongCount(position) == 0) {
                        ToastUtil.show(getActivity(),getString(R.string.list_is_empty));
                        return;
                    }
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", getPlayListId(position));
                    intent.putExtra("Title", name);
                    intent.putExtra("Type", Constants.PLAYLIST);
                    intent.putExtra("PlayListID", getPlayListId(position));
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String name = getPlayListName(position);
                if(!TextUtils.isEmpty(name))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,getPlayListId(position),TAG,Constants.PLAYLIST);
            }
        });
    }

    @Override
    protected void initView() {
        int model = SPUtil.getValue(getActivity(),"Setting","PlayListModel",Constants.GRID_MODEL);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(model == 1 ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
    }

    private int getPlayListId(int position){
        int playListId = -1;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            playListId = mAdapter.getDatas().get(position)._Id;
        }
        return playListId;
    }

    private String getPlayListName(int position){
        String playlistName = "";
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            playlistName = mAdapter.getDatas().get(position).Name;
        }
        return playlistName;
    }

    private int getPlayListSongCount(int position){
        int count = 0;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            count = mAdapter.getDatas().get(position).Count;
        }
        return count;
    }

    @Override
    public PlayListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onPlayListChanged() {
        onMediaStoreChanged();
    }

    @Override
    protected Loader<List<PlayList>> getLoader() {
        return new AsyncPlayListLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.PLAYLIST_FRAGMENT;
    }

    private static class AsyncPlayListLoader extends WrappedAsyncTaskLoader<List<PlayList>> {
        private AsyncPlayListLoader(Context context) {
            super(context);
        }

        @Override
        public List<PlayList> loadInBackground() {
            return PlayListUtil.getAllPlayListInfo();
        }
    }
}
