package remix.myplayer.ui.activity

import android.app.LoaderManager
import android.content.Loader
import android.os.Bundle
import remix.myplayer.ui.adapter.BaseAdapter
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.Constants

/**
 * Created by Remix on 2017/10/20.
 */
abstract class LibraryActivity<Data, Adapter : BaseAdapter<Data, *>?> : MenuActivity(), LoaderManager.LoaderCallbacks<List<Data>> {
  @JvmField
  protected var mAdapter: Adapter? = null
  protected var mChoice = MultipleChoice<Data>(this, Constants.SONG)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (hasPermission) {
      loaderManager.initLoader(loaderId, null, this)
    }
  }

  protected abstract val loaderId: Int
  protected abstract val loader: Loader<List<Data>>

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    if (hasPermission) {
      loaderManager.restartLoader(loaderId, null, this)
    } else {
      mAdapter?.setDataList(null)
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission) {
      hasPermission = has
      onMediaStoreChanged()
    }
  }

  override fun onPlayListChanged(name: String) {}

  override fun onLoadFinished(loader: Loader<List<Data>>, data: List<Data>?) {
    mAdapter?.setDataList(data)
  }

  override fun onLoaderReset(loader: Loader<List<Data>>) {
    mAdapter?.setDataList(null)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Data>> {
    return loader
  }

  override fun onBackPressed() {
    if (mChoice.isActive) {
      mChoice.close()
    } else {
      finish()
    }
  }
}