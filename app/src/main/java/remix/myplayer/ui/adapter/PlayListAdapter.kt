package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.R
import remix.myplayer.databinding.ItemPlaylistRecycleGridBinding
import remix.myplayer.databinding.ItemPlaylistRecycleListBinding
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.menu.LibraryListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.PlayListUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.request.UriRequest
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.libraryBtnColor
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.ui.adapter.holder.HeaderHolder
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller
import remix.myplayer.util.Constants
import remix.myplayer.util.ToastUtil

/**
 * Created by taeja on 16-1-15.
 */
/**
 * 播放列表的适配器
 */
class PlayListAdapter(layoutId: Int, multiChoice: MultipleChoice<PlayList>, recyclerView: RecyclerView)
  : HeaderAdapter<PlayList, BaseViewHolder>(layoutId, multiChoice, recyclerView), FastScroller.SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    if (viewType == TYPE_HEADER) {
      return HeaderHolder(
          LayoutInflater.from(parent.context).inflate(R.layout.layout_header_2, parent, false))
    }
    return if (viewType == LIST_MODE) PlayListListHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_recycle_list, parent, false))
    else PlayListGridHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_recycle_grid, parent, false))
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    disposeLoad(holder)
  }

  @SuppressLint("RestrictedApi")
  override fun convert(holder: BaseViewHolder, data: PlayList?, position: Int) {
    if (position == 0) {
      val headerHolder = holder as HeaderHolder
      setUpModeButton(headerHolder)
      return
    }
    if (holder !is PlayListHolder || data == null) {
      return
    }

    val context = holder.itemView.context
    holder.tvName.text = data.name
    holder.tvOther.text = context.getString(R.string.song_count, data.audioIds.size)

    //设置专辑封面
    val imageSize = if (mode == LIST_MODE) ImageUriRequest.SMALL_IMAGE_SIZE else ImageUriRequest.BIG_IMAGE_SIZE
    object : PlayListUriRequest(holder.iv,
        UriRequest(data.id, URL_PLAYLIST, UriRequest.TYPE_NETEASE_SONG),
        RequestConfig.Builder(imageSize, imageSize).build()) {}.load()
    holder.container.setOnClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnClickListener
      }
      onItemClickListener?.onItemClick(holder.container, position - 1)
    }

    //多选菜单
    holder.container.setOnLongClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnLongClickListener true
      }
      onItemClickListener?.onItemLongClick(holder.container, position - 1)
      true
    }
    Theme.tintDrawable(holder.btn,
        R.drawable.icon_player_more,
        libraryBtnColor)
    holder.btn.setOnClickListener { v: View? ->
      if (choice.isActive) {
        return@setOnClickListener
      }
      val popupMenu = PopupMenu(context, holder.btn)
      popupMenu.menuInflater.inflate(R.menu.menu_playlist_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(
          LibraryListener(context, data.id.toString() + "", Constants.PLAYLIST, data.name))
      popupMenu.show()
    }

    //是否处于选中状态
    holder.container.isSelected = choice.isPositionCheck(position - 1)
    setMarginForGridLayout(holder, position)
  }

  override fun getSectionText(position: Int): String {
    if (position == 0) {
      return ""
    }
    if (position - 1 < dataList.size) {
      val title = dataList[position - 1].name
      return if (!TextUtils.isEmpty(title)) Pinyin.toPinyin(title[0]).toUpperCase()
          .substring(0, 1) else ""
    }
    return ""
  }

  internal open class PlayListHolder(itemView: View) : BaseViewHolder(itemView) {
    lateinit var tvName: TextView
    lateinit var tvOther: TextView
    lateinit var iv: SimpleDraweeView
    lateinit var btn: ImageView
    lateinit var container: ViewGroup
  }

  internal class PlayListListHolder(itemView: View) : PlayListHolder(itemView) {
    init {
      val binding = ItemPlaylistRecycleListBinding.bind(itemView)
      tvName = binding.itemText1
      tvOther = binding.itemText2
      iv = binding.itemSimpleiview
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }

  internal class PlayListGridHolder(itemView: View) : PlayListHolder(itemView) {
    init {
      val binding = ItemPlaylistRecycleGridBinding.bind(itemView)
      tvName = binding.itemText1
      tvOther = binding.itemText2
      iv = binding.itemSimpleiview
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }
}