package remix.myplayer.ui.activity;

import android.os.Bundle;
import java.util.List;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.adapter.BaseAdapter;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2017/10/20.
 */

public abstract class LibraryActivity<D, A extends BaseAdapter> extends MenuActivity implements
    android.app.LoaderManager.LoaderCallbacks<List<D>> {

  protected A mAdapter;
  protected MultipleChoice<D> mChoice = new MultipleChoice<>(this, Constants.SONG);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (mHasPermission) {
      getLoaderManager().initLoader(getLoaderId(), null, this);
    }
  }

  protected abstract int getLoaderId();


  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();
    if (mHasPermission) {
      getLoaderManager().restartLoader(getLoaderId(), null, this);
    } else {
      if (mAdapter != null) {
        mAdapter.setData(null);
      }
    }
  }

  @Override
  public void onPermissionChanged(boolean has) {
    if (has != mHasPermission) {
      mHasPermission = has;
      onMediaStoreChanged();
    }
  }

  @Override
  public void onPlayListChanged(String name) {

  }

  @Override
  public android.content.Loader<List<D>> onCreateLoader(int id, Bundle args) {
    return getLoader();
  }

  @Override
  public void onLoadFinished(android.content.Loader<List<D>> loader, List<D> data) {
    if (mAdapter != null) {
      mAdapter.setData(data);
    }
  }

  @Override
  public void onLoaderReset(android.content.Loader<List<D>> loader) {
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

  protected android.content.Loader<List<D>> getLoader() {
    return null;
  }

  @Override
  public void onBackPressed() {
    if (mChoice.isActive()) {
      mChoice.close();
    } else {
      finish();
    }
  }
}
