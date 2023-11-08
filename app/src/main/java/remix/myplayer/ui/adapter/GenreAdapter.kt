package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.databinding.ItemGenreRecycleGridBinding
import remix.myplayer.databinding.ItemGenreRecycleListBinding
import remix.myplayer.glide.GlideApp
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.menu.LibraryListener
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.ui.adapter.holder.HeaderHolder
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller
import remix.myplayer.util.Constants
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import java.util.Locale

class GenreAdapter(
  layoutId: Int,
  multiChoice: MultipleChoice<Genre>,
  recyclerView: FastScrollRecyclerView
) : HeaderAdapter<Genre, BaseViewHolder>(layoutId, multiChoice, recyclerView),
  FastScroller.SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    if (viewType == TYPE_HEADER) {
      return HeaderHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.layout_header_2, parent, false)
      )
    }
    return if (viewType == LIST_MODE) GenreListHolder(
      ItemGenreRecycleListBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
      )
    )
    else GenreGridHolder(
      ItemGenreRecycleGridBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )
  }

  @SuppressLint("RestrictedApi", "CheckResult")
  override fun convert(holder: BaseViewHolder, genre: Genre?, position: Int) {
    if (position == 0) {
      val headerHolder = holder as HeaderHolder
      setUpModeButton(headerHolder)
      return
    }

    if (holder !is GenreHolder || genre == null) {
      return
    }
    val context = holder.itemView.context
    //设置歌手名
    holder.tv1.text = genre.genre
    if (holder is GenreListHolder) {
      if (genre.count > 0) {
        holder.tv2.text = context.getString(R.string.song_count_1, genre.count)
      } else {
        holder.tv2.text = App.context.getString(R.string.song_count_1, genre.count)
      }
    }
    //设置封面
    val options = RequestOptions()
      .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
      .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))

    if (mode == GRID_MODE) {
      options.transform(MultiTransformation(CenterCrop(), RoundedCorners(DensityUtil.dip2px(2f))))
    }

    GlideApp.with(holder.itemView)
      .load(genre)
      .apply(options)
      .into(holder.iv)

    holder.container.setOnClickListener { v: View? ->
      if (holder.adapterPosition - 1 < 0) {
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

    //popupmenu
    val tintColor = ThemeStore.libraryBtnColor
    Theme.tintDrawable(holder.btn, R.drawable.icon_player_more, tintColor)
    holder.btn.setOnClickListener { v: View? ->
      if (choice.isActive) {
        return@setOnClickListener
      }
      val popupMenu = PopupMenu(context, holder.btn)
      popupMenu.menuInflater.inflate(R.menu.menu_genre_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(
        LibraryListener(
          context, genre,
          Constants.GENRE,
          genre.genre
        )
      )
      popupMenu.gravity = Gravity.END
      popupMenu.show()
    }

    //是否处于选中状态
    holder.container.isSelected = choice.isPositionCheck(position - 1)

    //设置padding
    setMarginForGridLayout(holder, position)
  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val data = dataList[position - 1]
      val key = when (SPUtil.getValue(
        App.context,
        SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.GENRE_SORT_ORDER,
        SortOrder.GENRE_A_Z
      )) {
        SortOrder.GENRE_A_Z, SortOrder.GENRE_Z_A -> data.genre
        else -> ""
      }
      if (key.isNotEmpty())
        return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
    }
    return ""
  }

  internal open class GenreHolder(v: View) : BaseViewHolder(v) {
    lateinit var tv1: TextView
    lateinit var tv2: TextView
    lateinit var iv: ImageView
    lateinit var btn: ImageButton
    lateinit var container: ViewGroup
  }

  internal class GenreListHolder(binding: ItemGenreRecycleListBinding) : GenreHolder(binding.root) {
    init {
      tv1 = binding.itemText1
      tv2 = binding.itemText2
      iv = binding.iv
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }

  internal class GenreGridHolder(binding: ItemGenreRecycleGridBinding) : GenreHolder(binding.root) {
    init {
      tv1 = binding.itemText1
      iv = binding.iv
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }
}