package remix.myplayer.ui.fragment

import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_playlist.*
import remix.myplayer.App
import remix.myplayer.R
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
class PlayListFragment : LibraryFragment<PlayList, PlayListAdapter>() {
  override val layoutID: Int = R.layout.fragment_playlist

  override fun initAdapter() {
    adapter = PlayListAdapter(R.layout.item_playlist_recycle_grid, multiChoice, recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val playList = adapter.dataList[position]
        if ((!TextUtils.isEmpty(playList.name) && userVisibleHint) && !multiChoice.click(position, playList)) {
          if (playList.audioIds.isEmpty()) {
            ToastUtil.show(requireContext(), getStringSafely(R.string.list_is_empty))
            return
          }
          ChildHolderActivity.start(requireContext(), Constants.PLAYLIST, playList.id.toString(), playList.name)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          multiChoice.longClick(position, adapter?.dataList?.get(position))
        }
      }
    }
  }

  override fun initView() {
    val model = SPUtil.getValue(requireContext(), SETTING_KEY.NAME, SETTING_KEY.MODE_FOR_PLAYLIST, HeaderAdapter.GRID_MODE)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(activity, spanCount)
    recyclerView.adapter = adapter
    recyclerView.setHasFixedSize(true)
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

  class AsyncPlayListLoader(context: Context) : WrappedAsyncTaskLoader<List<PlayList>>(context) {
    override fun loadInBackground(): List<PlayList> {
      val sortOrder = SPUtil.getValue(
          App.context,
          SETTING_KEY.NAME,
          SETTING_KEY.PLAYLIST_SORT_ORDER,
          SortOrder.PLAYLIST_A_Z
      )
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