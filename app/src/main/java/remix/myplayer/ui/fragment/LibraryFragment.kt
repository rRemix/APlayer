package remix.myplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.adapter.BaseAdapter
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.Constants
import remix.myplayer.util.DensityUtil

/**
 * Created by Remix on 2016/12/23.
 */
abstract class LibraryFragment<D, A : BaseAdapter<D, *>?> : BaseMusicFragment(), MusicEventCallback, LoaderManager.LoaderCallbacks<List<D>> {
  @JvmField
  protected var mAdapter: A? = null
  @JvmField
  protected var mChoice: MultipleChoice<D>? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val type = if (this is SongFragment) Constants.SONG else if (this is AlbumFragment) Constants.ALBUM else if (this is ArtistFragment) Constants.ARTIST else if (this is PlayListFragment) Constants.PLAYLIST else Constants.FOLDER
    mChoice = MultipleChoice(requireActivity(), type)
    initAdapter()
    initView()

    //recyclerView的滚动条
    val accentColor = ThemeStore.accentColor
    val recyclerView: RecyclerView? = view.findViewById(R.id.recyclerView)
    if (recyclerView is FastScrollRecyclerView) {
      recyclerView.setBubbleColor(accentColor)
      recyclerView.setHandleColor(accentColor)
      recyclerView.setBubbleTextColor(ColorUtil.getColor(if (ColorUtil.isColorLight(accentColor)) R.color.light_text_color_primary else R.color.dark_text_color_primary))
    }
    mChoice?.adapter = mAdapter

    if (mHasPermission) {
      loaderManager.initLoader(loaderId, null, this)
    }
  }

  val choice: MultipleChoice<*>?
    get() = mChoice

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(layoutID, container, false)
  }

  protected abstract val layoutID: Int
  protected abstract fun initAdapter()
  protected abstract fun initView()
  override fun onDestroy() {
    super.onDestroy()
    mAdapter?.setData(null)
  }

  override fun onMediaStoreChanged() {
    if (mHasPermission) {
      loaderManager.restartLoader(loaderId, null, this)
    } else {
      mAdapter?.setData(null)
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != mHasPermission) {
      mHasPermission = has
      onMediaStoreChanged()
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<D>> {
    return loader()
  }

  override fun onLoadFinished(loader: Loader<List<D>>, data: List<D>?) {
    mAdapter?.setData(data)
  }

  override fun onLoaderReset(loader: Loader<List<D>>) {
    mAdapter?.setData(null)
  }

  protected val spanCount: Int
    get() {
      val portraitOrientation = requireContext().isPortraitOrientation()
      return if (portraitOrientation) {
        PORTRAIT_ORIENTATION_COUNT
      } else {
        val count = resources.displayMetrics.widthPixels / LANDSCAPE_ORIENTATION_ITEM_WIDTH
        if (count > PORTRAIT_ORIENTATION_MAX_ITEM_COUNT) PORTRAIT_ORIENTATION_MAX_ITEM_COUNT else count
      }
    }

  protected abstract fun loader():Loader<List<D>>
  protected abstract val loaderId: Int

  companion object {
    private const val PORTRAIT_ORIENTATION_COUNT = 2
    private val LANDSCAPE_ORIENTATION_ITEM_WIDTH = DensityUtil.dip2px(180f)
    private const val PORTRAIT_ORIENTATION_MAX_ITEM_COUNT = 6
  }
}