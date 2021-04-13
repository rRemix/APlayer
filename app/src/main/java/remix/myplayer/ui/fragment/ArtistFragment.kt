package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_artist.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.activity.ChildHolderActivity
import remix.myplayer.ui.adapter.ArtistAdapter
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil.getAllArtist
import remix.myplayer.util.SPUtil

/**
 * Created by Remix on 2015/12/22.
 */
/**
 * 艺术家Fragment
 */
class ArtistFragment : LibraryFragment<Artist, ArtistAdapter>() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_artist

  override fun initAdapter() {
    mAdapter = ArtistAdapter(R.layout.item_artist_recycle_grid, mChoice, recyclerView)
    mAdapter?.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val artist = mAdapter?.datas?.get(position) ?: return
        if (userVisibleHint && mChoice?.click(position, artist) == false) {
          mContext?.let { ChildHolderActivity.start(it, Constants.ARTIST, artist.artistID.toString(), artist.artist) }
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
    val model = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST, HeaderAdapter.GRID_MODE)
    recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(mContext) else GridLayoutManager(activity, spanCount)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.adapter = mAdapter
    recyclerView.setHasFixedSize(true)
  }

  override fun loader(): Loader<List<Artist>> {
    return AsyncArtistLoader(mContext)
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_ARTIST

  override val adapter: ArtistAdapter? = mAdapter

  private class AsyncArtistLoader(context: Context?) : WrappedAsyncTaskLoader<List<Artist>>(context) {
    override fun loadInBackground(): List<Artist> {
      return getAllArtist()
    }
  }

  companion object {
    val TAG = ArtistFragment::class.java.simpleName
  }
}