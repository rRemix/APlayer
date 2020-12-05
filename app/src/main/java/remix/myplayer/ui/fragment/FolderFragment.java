package remix.myplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import butterknife.BindView;
import java.util.List;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.adapter.FolderAdapter;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 文件夹Fragment
 */
public class FolderFragment extends LibraryFragment<Folder, FolderAdapter> {

  @BindView(R.id.recyclerView)
  RecyclerView mRecyclerView;

  public static final String TAG = FolderFragment.class.getSimpleName();

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_folder;
  }

  @Override
  protected void initAdapter() {
    mAdapter = new FolderAdapter(R.layout.item_folder_recycle, mChoice);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        Folder folder = mAdapter.getDatas().get(position);
        String path = folder.getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path) &&
            !mChoice.click(position, folder)) {
          ChildHolderActivity.start(mContext, Constants.FOLDER, folder.getParentId(), path);
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        Folder folder = mAdapter.getDatas().get(position);
        String path = mAdapter.getDatas().get(position).getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path)) {
          mChoice.longClick(position, folder);
        }
      }
    });
  }

  @Override
  protected void initView() {
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  protected Loader<List<Folder>> getLoader() {
    return new AsyncFolderLoader(mContext);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.FOLDER_FRAGMENT;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = TAG;
  }

  @Override
  public FolderAdapter getAdapter() {
    return mAdapter;
  }

  private static class AsyncFolderLoader extends WrappedAsyncTaskLoader<List<Folder>> {

    private AsyncFolderLoader(Context context) {
      super(context);
    }

    @Override
    public List<Folder> loadInBackground() {
      return MediaStoreUtil.getAllFolder();
    }
  }
}
