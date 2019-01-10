package remix.myplayer.misc.asynctask;

import android.content.AsyncTaskLoader;
import android.content.Context;

/**
 * Created by Remix on 2017/10/22.
 */

public abstract class AppWrappedAsyncTaskLoader<D> extends AsyncTaskLoader<D> {

  private D mData;

  /**
   * Constructor of <code>WrappedAsyncTaskLoader</code>
   *
   * @param context The {@link Context} to use.
   */
  public AppWrappedAsyncTaskLoader(Context context) {
    super(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deliverResult(D data) {
    if (!isReset()) {
      this.mData = data;
      super.deliverResult(data);
    } else {
      // An asynchronous query came in while the loader is stopped
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onStartLoading() {
    super.onStartLoading();
    if (this.mData != null) {
      deliverResult(this.mData);
    } else if (takeContentChanged() || this.mData == null) {
      forceLoad();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onStopLoading() {
    super.onStopLoading();
    // Attempt to cancel the current load task if possible
    cancelLoad();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onReset() {
    super.onReset();
    // Ensure the loader is stopped
    onStopLoading();
    this.mData = null;
  }
}
