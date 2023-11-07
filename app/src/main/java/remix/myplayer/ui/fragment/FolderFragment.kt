package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_folder.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.activity.ChildHolderActivity
import remix.myplayer.ui.adapter.FolderAdapter
import remix.myplayer.util.Constants
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.MediaStoreUtil.getAllFolder

/**
 * Created by Remix on 2015/12/5.
 */
/**
 * 文件夹Fragment
 */
class FolderFragment : LibraryFragment<Folder, FolderAdapter>() {

  override val layoutID = R.layout.fragment_folder

  override fun initAdapter() {
    adapter = FolderAdapter(R.layout.item_folder_recycle, multiChoice)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val folder = adapter.dataList[position]
        val path = folder.path
        if (userVisibleHint && !TextUtils.isEmpty(path) && !multiChoice.click(position, folder)) {
          ChildHolderActivity.start(requireContext(), Constants.FOLDER, folder.path, path)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        val folder = adapter.dataList[position]
        val path = adapter.dataList[position].path
        if (userVisibleHint && !TextUtils.isEmpty(path)) {
          multiChoice.longClick(position, folder)
        }
      }
    }
  }

  override fun initView() {
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.setHasFixedSize(true)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.adapter = adapter
  }

  override fun loader(): Loader<List<Folder>> {
    return AsyncFolderLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_FOLDER

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  private class AsyncFolderLoader(context: Context?) : WrappedAsyncTaskLoader<List<Folder>>(context) {
    override fun loadInBackground(): List<Folder> {
      return getAllFolder().sortedBy { it.name }
    }
  }

  companion object {
    val TAG = FolderFragment::class.java.simpleName
  }
}