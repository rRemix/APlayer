package remix.myplayer.ui.fragment;

import static remix.myplayer.ui.adapter.HeaderAdapter.LIST_MODE;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import butterknife.BindView;
import java.util.List;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.adapter.ArtistAdapter;
import remix.myplayer.ui.adapter.HeaderAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家Fragment
 */
public class ArtistFragment extends LibraryFragment<Artist, ArtistAdapter> {

  @BindView(R.id.recyclerView)
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
    mAdapter = new ArtistAdapter(R.layout.item_artist_recycle_grid, mChoice, mRecyclerView);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Artist artist = mAdapter.getDatas().get(position);
        if (getUserVisibleHint() && artist != null &&
            !mChoice.click(position, artist)) {
          if (mAdapter.getDatas() != null) {
            ChildHolderActivity
                .start(mContext, Constants.ARTIST, artist.getArtistID(), artist.getArtist());
          }
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        if (getUserVisibleHint()) {
          mChoice.longClick(position, mAdapter.getDatas().get(position));
        }
      }
    });
  }

  @Override
  protected void initView() {
    int model = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST,
            HeaderAdapter.GRID_MODE);
    mRecyclerView.setLayoutManager(model == LIST_MODE ? new LinearLayoutManager(mContext)
        : new GridLayoutManager(getActivity(), getSpanCount()));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setHasFixedSize(true);
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
  public ArtistAdapter getAdapter() {
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
