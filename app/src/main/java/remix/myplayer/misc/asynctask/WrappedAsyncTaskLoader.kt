package remix.myplayer.misc.asynctask

import android.content.Context
import androidx.loader.content.AsyncTaskLoader

/**
 * [Issue
 * 14944](http://code.google.com/p/android/issues/detail?id=14944)
 *
 * @author Alexander Blom
 */

/**
 * Constructor of `WrappedAsyncTaskLoader`
 *
 * @param context The [Context] to use.
 */
abstract class WrappedAsyncTaskLoader<D>(context: Context) : AsyncTaskLoader<D>(context) {
  private var data: D? = null

  /**
   * {@inheritDoc}
   */
  override fun deliverResult(data: D?) {
    if (!isReset) {
      this.data = data
      super.deliverResult(data)
    } else {
      // An asynchronous query came in while the loader is stopped
    }
  }

  /**
   * {@inheritDoc}
   */
  override fun onStartLoading() {
    super.onStartLoading()
    if (data != null) {
      deliverResult(data)
    } else if (takeContentChanged() || data == null) {
      forceLoad()
    }
  }

  /**
   * {@inheritDoc}
   */
  override fun onStopLoading() {
    super.onStopLoading()
    // Attempt to cancel the current load task if possible
    cancelLoad()
  }

  /**
   * {@inheritDoc}
   */
  override fun onReset() {
    super.onReset()
    // Ensure the loader is stopped
    onStopLoading()
    data = null
  }
}