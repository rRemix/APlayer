package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.databinding.ItemPlaylistRecycleGridBinding
import remix.myplayer.databinding.ItemPlaylistRecycleListBinding
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.glide.GlideApp
import remix.myplayer.helper.SortOrder
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
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil
import java.util.*

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
//    val imageSize = if (mode == LIST_MODE) ImageUriRequest.SMALL_IMAGE_SIZE else ImageUriRequest.BIG_IMAGE_SIZE
//    object : PlayListUriRequest(holder.iv,
//        UriRequest(data.id, URL_PLAYLIST, UriRequest.TYPE_NETEASE_SONG),
//        RequestConfig.Builder(imageSize, imageSize).build()) {}.load()
    GlideApp.with(holder.itemView)
        .load(data)
        .centerCrop()
        .into(holder.iv)

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
    if (position in 1..dataList.size) {
      val data = dataList[position - 1]
      val key = when (SPUtil.getValue(
        App.context,
        SETTING_KEY.NAME,
        SETTING_KEY.PLAYLIST_SORT_ORDER,
        SortOrder.PLAYLIST_A_Z
      )) {
        SortOrder.PLAYLIST_A_Z, SortOrder.PLAYLIST_Z_A -> data.name
        else -> ""
      }
      if (key.isNotEmpty())
        return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
    }
    return ""
  }

  internal open class PlayListHolder(itemView: View) : BaseViewHolder(itemView) {
    lateinit var tvName: TextView
    lateinit var tvOther: TextView
    lateinit var iv: ImageView
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
      iv = binding.iv
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }
}