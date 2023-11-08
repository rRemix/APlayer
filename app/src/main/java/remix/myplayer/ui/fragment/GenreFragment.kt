package remix.myplayer.ui.fragment

import android.content.Context
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_artist.recyclerView
import remix.myplayer.R
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.activity.ChildHolderActivity
import remix.myplayer.ui.adapter.GenreAdapter
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.SPUtil

class GenreFragment: LibraryFragment<Genre, GenreAdapter>() {

  override val layoutID: Int = R.layout.fragment_genre

  override fun initAdapter() {
    adapter = GenreAdapter(R.layout.item_genre_recycle_grid, multiChoice, recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val genre = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, genre)) {
          ChildHolderActivity.start(requireContext(), Constants.GENRE, genre.id.toString(), genre.genre)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          multiChoice.longClick(position, adapter.dataList[position])
        }
      }
    }
  }

  override fun initView() {
    val model = SPUtil.getValue(requireContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_GENRE, HeaderAdapter.GRID_MODE)
    recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(activity, spanCount)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.adapter = adapter
    recyclerView.setHasFixedSize(true)
  }

  override fun loader(): Loader<List<Genre>> {
    return AsyncGenreLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_GENRE

  private class AsyncGenreLoader(context: Context?) : WrappedAsyncTaskLoader<List<Genre>>(context) {
    override fun loadInBackground(): List<Genre> {
      return MediaStoreUtil.getAllGenres()
    }
  }

  companion object {
    val TAG = ArtistFragment::class.java.simpleName
  }
}