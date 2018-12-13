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
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.adapter.HeaderAdapter;
import remix.myplayer.ui.adapter.PlayListAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.ui.adapter.HeaderAdapter.LIST_MODE;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class PlayListFragment extends LibraryFragment<PlayList, PlayListAdapter> {
    public static final String TAG = PlayListFragment.class.getSimpleName();
    @BindView(R.id.recyclerView)
    FastScrollRecyclerView mRecyclerView;

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_playlist;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new PlayListAdapter(mContext, R.layout.item_playlist_recycle_grid, mChoice,mRecyclerView);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final PlayList playList = getAdapter().getDatas().get(position);
                String name = getPlayListName(position);
                if (!TextUtils.isEmpty(name) && getUserVisibleHint() && !mChoice.click(position, playList)) {
                    if (getPlayListSongCount(position) == 0) {
                        ToastUtil.show(mContext, getStringSafely(R.string.list_is_empty));
                        return;
                    }
                    Intent intent = new Intent(mContext, ChildHolderActivity.class);
                    intent.putExtra("Id", getPlayListId(position));
                    intent.putExtra("Title", name);
                    intent.putExtra("Type", Constants.PLAYLIST);
                    intent.putExtra("PlayListID", getPlayListId(position));
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (getUserVisibleHint())
                    mChoice.longClick(position, mAdapter.getDatas().get(position));
            }
        });
    }

    @Override
    protected void initView() {
        int model = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST, HeaderAdapter.GRID_MODE);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(model == LIST_MODE ? new LinearLayoutManager(mContext) : new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
    }

    private int getPlayListId(int position) {
        int playListId = -1;
        if (mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1) {
            playListId = mAdapter.getDatas().get(position)._Id;
        }
        return playListId;
    }

    private String getPlayListName(int position) {
        String playlistName = "";
        if (mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1) {
            playlistName = mAdapter.getDatas().get(position).Name;
        }
        return playlistName;
    }

    private int getPlayListSongCount(int position) {
        int count = 0;
        if (mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1) {
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
