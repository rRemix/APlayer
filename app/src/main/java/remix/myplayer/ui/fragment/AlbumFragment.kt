package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import kotlinx.android.synthetic.main.fragment_album.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Album
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.activity.ChildHolderActivity
import remix.myplayer.ui.adapter.AlbumAdapter
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil.getAllAlbum
import remix.myplayer.util.SPUtil

/**
 * Created by Remix on 2015/12/20.
 */
/**
 * 专辑Fragment
 */
class AlbumFragment : LibraryFragment<Album, AlbumAdapter>() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_album

  override fun initAdapter() {
    mAdapter = AlbumAdapter(R.layout.item_album_recycle_grid, mChoice, recyclerView)
    mAdapter?.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val album = mAdapter?.datas?.get(position) ?: return
        if (userVisibleHint && mChoice?.click(position, album) == false) {
          ChildHolderActivity.start(mContext, Constants.ALBUM, album.albumID, album.album)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          mChoice?.longClick(position, mAdapter?.datas?.get(position))
        }
      }
    })
  }

  override fun initView() {
    val mode = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ALBUM, HeaderAdapter.GRID_MODE)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.layoutManager = if (mode == HeaderAdapter.LIST_MODE) LinearLayoutManager(mContext) else GridLayoutManager(mContext, spanCount)
    recyclerView.adapter = mAdapter
    recyclerView.setHasFixedSize(true)
  }

  override val adapter: AlbumAdapter? = mAdapter

  override fun loader(): Loader<List<Album>> {
    return AsyncAlbumLoader(mContext)
  }

  override val loaderId: Int = LoaderIds.ALBUM_FRAGMENT

  private class AsyncAlbumLoader(context: Context?) : WrappedAsyncTaskLoader<List<Album>>(context) {
    override fun loadInBackground(): List<Album> {
      return getAllAlbum()
    }
  }

  companion object {
    val TAG = AlbumFragment::class.java.simpleName
  }
}