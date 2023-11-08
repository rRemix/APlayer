package remix.myplayer.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.misc.isPortraitOrientation
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.ui.adapter.holder.HeaderHolder
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.SPUtil
import java.lang.IllegalStateException

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */
abstract class HeaderAdapter<Data, ViewHolder : RecyclerView.ViewHolder>(
    layoutId: Int,
    var choice: MultipleChoice<Data>,
    var recyclerView: RecyclerView) : BaseAdapter<Data, BaseViewHolder>(layoutId) {

  //当前列表模式 1:列表 2:网格
  @JvmField
  var mode = GRID_MODE
  override fun getItemViewType(position: Int): Int {
    return if (position == 0) {
      TYPE_HEADER
    } else mode
  }

  override fun getItem(position: Int): Data? {
    return if (position == 0) null else if (position - 1 < dataList.size) dataList[position - 1] else null
  }

  override fun getItemCount(): Int {
    return super.getItemCount() + 1
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    val manager = recyclerView.layoutManager
    if (manager is GridLayoutManager) {
      manager.spanSizeLookup = object : SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
          return if (getItemViewType(position) == TYPE_HEADER) manager.spanCount else 1
        }
      }
    }
  }

  /**
   * 初始化列表模式切换的按钮
   */
  fun setUpModeButton(headerHolder: HeaderHolder) {
    if (dataList.size == 0) {
      headerHolder.binding.root.visibility = View.GONE
      return
    }
    headerHolder.binding.root.visibility = View.VISIBLE
    //设置图标
    headerHolder.binding.divider.visibility = if (mode == LIST_MODE) View.VISIBLE else View.GONE
    headerHolder.binding.gridModel.setOnClickListener { v: View -> switchMode(headerHolder, v) }
    headerHolder.binding.listModel.setOnClickListener { v: View -> switchMode(headerHolder, v) }
    headerHolder.binding.divider.visibility = if (mode == LIST_MODE) View.VISIBLE else View.GONE
    tintModeButton(headerHolder)
  }

  /**
   * 列表模式切换
   */
  private fun switchMode(headerHolder: HeaderHolder, v: View) {
    val newModel = if (v.id == R.id.list_model) LIST_MODE else GRID_MODE
    if (newModel == mode) {
      return
    }
    mode = newModel
    setUpModeButton(headerHolder)
    //重新设置LayoutManager和adapter并刷新列表
    recyclerView.layoutManager = if (mode == LIST_MODE) LinearLayoutManager(headerHolder.itemView.context) else GridLayoutManager(headerHolder.itemView.context, 2)
    recyclerView.adapter = this
    //保存当前模式
    saveMode(headerHolder.itemView.context)
  }

  private fun tintModeButton(headerHolder: HeaderHolder) {
    headerHolder.binding.listModel.setImageDrawable(
        Theme.tintVectorDrawable(headerHolder.itemView.context,
            R.drawable.ic_format_list_bulleted_white_24dp,
            if (mode == LIST_MODE) accentColor else ColorUtil.getColor(R.color.default_model_button_color))
    )
    headerHolder.binding.gridModel.setImageDrawable(
        Theme.tintVectorDrawable(headerHolder.itemView.context, R.drawable.ic_apps_white_24dp,
            if (mode == GRID_MODE) accentColor else ColorUtil.getColor(R.color.default_model_button_color))
    )
  }

  private fun saveMode(context: Context) {
    SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, key ?: return, mode)
  }

  fun setMarginForGridLayout(holder: BaseViewHolder, position: Int) {
    //设置margin,当作Divider
    if (mode == GRID_MODE && holder.mRoot != null) {
      val lp = holder.mRoot
          .layoutParams as MarginLayoutParams
      if (holder.itemView.context.isPortraitOrientation()) { //竖屏
        if (position % 2 == 1) {
          lp.setMargins(GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL)
        } else {
          lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL)
        }
      } else { //横屏
        lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2,
            GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2)
      }
      holder.mRoot.layoutParams = lp
    }
  }

//  fun setImage(simpleDraweeView: SimpleDraweeView,
//               uriRequest: UriRequest,
//               imageSize: Int,
//               position: Int): Disposable {
//    return object : LibraryUriRequest(simpleDraweeView,
//        uriRequest,
//        RequestConfig.Builder(imageSize, imageSize).build()) {}.load()
//  }

//  fun disposeLoad(holder: RecyclerView.ViewHolder) {
//    val parent = if (holder.itemView is ViewGroup) holder.itemView as ViewGroup else null
//    if (parent != null) {
//      for (i in 0 until parent.childCount) {
//        val childView = parent.getChildAt(i)
//        if (childView is SimpleDraweeView) {
//          val tag = childView.getTag()
//          if (tag is Disposable) {
//            if (!tag.isDisposed) {
//              tag.dispose()
//            }
//            childView.setTag(null)
//          }
//        }
//      }
//    }
//  }

  companion object {
    //显示模式 1:列表 2:网格
    const val LIST_MODE = 1
    const val GRID_MODE = 2

    //网格模式下水平和垂直的间距 以间距当作Divider
    private val GRID_MARGIN_VERTICAL = DensityUtil.dip2px(4f)
    private val GRID_MARGIN_HORIZONTAL = DensityUtil.dip2px(6f)
    const val TYPE_HEADER = 0
    const val TYPE_NORMAL = 1
  }

  val key by lazy {
    when (this) {
      is AlbumAdapter -> {
        SPUtil.SETTING_KEY.MODE_FOR_ALBUM
      }

      is ArtistAdapter -> {
        SPUtil.SETTING_KEY.MODE_FOR_ARTIST
      }

      is GenreAdapter -> {
        SPUtil.SETTING_KEY.MODE_FOR_GENRE
      }

      is PlayListAdapter -> {
        SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST
      }

      else -> {
        null
      }
    }
  }

  init {
    //其他的列表都是List模式
    mode = if (key != null) SPUtil
        .getValue(recyclerView.context, SPUtil.SETTING_KEY.NAME, key, GRID_MODE) else LIST_MODE
  }
}