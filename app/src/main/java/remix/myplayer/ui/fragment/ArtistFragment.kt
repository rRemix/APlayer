package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.databinding.FragmentArtistBinding
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
class ArtistFragment : LibraryFragment<Artist, ArtistAdapter, FragmentArtistBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentArtistBinding
    get() = FragmentArtistBinding::inflate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  override fun initAdapter() {
    adapter = ArtistAdapter(R.layout.item_artist_recycle_grid, multiChoice, binding.recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val artist = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, artist)) {
          ChildHolderActivity.start(requireContext(), Constants.ARTIST, artist.artistID.toString(), artist.artist)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          multiChoice.longClick(position, adapter.dataList.get(position))
        }
      }
    }
  }

  override fun initView() {
    val model = SPUtil.getValue(requireContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST, HeaderAdapter.GRID_MODE)
    binding.recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(activity, spanCount)
    binding.recyclerView.itemAnimator = DefaultItemAnimator()
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setHasFixedSize(true)
  }

  override fun loader(): Loader<List<Artist>> {
    return AsyncArtistLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_ARTIST

  private class AsyncArtistLoader(context: Context?) : WrappedAsyncTaskLoader<List<Artist>>(context) {
    override fun loadInBackground(): List<Artist> {
      return getAllArtist()
    }
  }

  companion object {
    val TAG = ArtistFragment::class.java.simpleName
  }
}