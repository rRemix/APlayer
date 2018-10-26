package remix.myplayer.ui.fragment

import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import butterknife.ButterKnife
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.ui.adapter.BaseAdapter
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.ui.multiple.Controller
import remix.myplayer.ui.multiple.MultipleChoice
import remix.myplayer.util.Constants

/**
 * Created by Remix on 2016/12/23.
 */

abstract class LibraryFragment<D, A : BaseAdapter<*, *>> : BaseMusicFragment(), MusicEventCallback, LoaderManager.LoaderCallbacks<List<D>> {
    protected var mAdapter: A? = null
    protected var mChoice: MultipleChoice<D>

    val choice: MultipleChoice<*>
        get() = mChoice


    protected abstract val layoutID: Int

    protected abstract val loader: Loader<List<D>>

    protected abstract val loaderId: Int

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mHasPermission) {
            loaderManager.initLoader(loaderId, null, this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(layoutID, container, false)
        mUnBinder = ButterKnife.bind(this, rootView)


        mChoice = MultipleChoice(activity, Constants.ALBUM)
        initAdapter()
        initView()

        mChoice.adapter = mAdapter
        return rootView
    }

    protected abstract fun initAdapter()

    protected abstract fun initView()

    override fun onDestroy() {
        super.onDestroy()
        if (mAdapter != null) {
            mAdapter!!.setData(null)
        }
    }

    override fun onMediaStoreChanged() {
        if (mHasPermission)
            loaderManager.restartLoader(loaderId, null, this)
        else {
            if (mAdapter != null)
                mAdapter!!.setData(null)
        }
    }

    override fun onPermissionChanged(has: Boolean) {
        if (has != mHasPermission) {
            mHasPermission = has
            onMediaStoreChanged()
        }
    }

    override fun onPlayListChanged() {

    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<D>> {
        return loader
    }

    override fun onLoadFinished(loader: Loader<List<D>>, data: List<D>) {
        mAdapter!!.setData(data)
    }

    override fun onLoaderReset(loader: Loader<List<D>>) {
        if (mAdapter != null)
            mAdapter!!.setData(null)
    }

}
