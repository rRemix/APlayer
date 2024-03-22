package remix.myplayer.ui.fragment

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.databinding.FragmentPlaylistBinding
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.activity.ChildHolderActivity
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.ui.adapter.PlayListAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
class PlayListFragment : LibraryFragment<PlayList, PlayListAdapter, FragmentPlaylistBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlaylistBinding
    get() = FragmentPlaylistBinding::inflate

  override fun initAdapter() {
    adapter = PlayListAdapter(R.layout.item_playlist_recycle_grid, multiChoice, binding.recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val playList = adapter.dataList[position]
        if ((!TextUtils.isEmpty(playList.name) && userVisibleHint) && !multiChoice.click(position, playList)) {
          if (playList.audioIds.isEmpty()) {
            ToastUtil.show(requireContext(), getStringSafely(R.string.list_is_empty))
            return
          }
          ChildHolderActivity.start(requireContext(), Constants.PLAYLIST, playList.id.toString(), playList.name, playList)
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
    val model = SPUtil.getValue(requireContext(), SETTING_KEY.NAME, SETTING_KEY.MODE_FOR_PLAYLIST, HeaderAdapter.GRID_MODE)
    binding.recyclerView.itemAnimator = DefaultItemAnimator()
    binding.recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(activity, spanCount)
    binding.recyclerView.adapter = adapter
    binding.recyclerView.setHasFixedSize(true)
  }

  override fun onPlayListChanged(name: String) {
    if (name == PlayList.TABLE_NAME) {
      onMediaStoreChanged()
    }
  }

  override fun loader(): Loader<List<PlayList>> {
    return AsyncPlayListLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_PLAYLIST

  class AsyncPlayListLoader(context: Context?) : WrappedAsyncTaskLoader<List<PlayList>>(context) {
    override fun loadInBackground(): List<PlayList> {
      var sortOrder = SPUtil.getValue(
          App.context,
          SETTING_KEY.NAME,
          SETTING_KEY.PLAYLIST_SORT_ORDER,
          SortOrder.PLAYLIST_DATE
      )
      sortOrder = when(sortOrder) {
        SortOrder.PLAYLIST_A_Z -> {
          "name"
        }
        SortOrder.PLAYLIST_Z_A -> {
          "name DESC"
        }
        else -> {
          sortOrder
        }
      }
      val forceSort =
          SPUtil.getValue(App.context, SETTING_KEY.NAME, SETTING_KEY.FORCE_SORT, false)
      return getInstance().getSortPlayList("SELECT * FROM PlayList ORDER BY $sortOrder")
          .blockingGet().let {
            if (forceSort) {
              ItemsSorter.sortedPlayLists(it, sortOrder)
            } else {
              it
            }
          }
    }
  }

  companion object {
    val TAG = PlayListFragment::class.java.simpleName
  }
}