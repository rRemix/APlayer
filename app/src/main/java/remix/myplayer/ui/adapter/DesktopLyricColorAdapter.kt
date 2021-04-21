package remix.myplayer.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.databinding.ItemFloatLrcColorBinding
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.ThemeStore.floatLyricTextColor
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter.FloatColorHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil

/**
 * Created by Remix on 2017/8/15.
 */
class DesktopLyricColorAdapter(context: Context?, layoutId: Int, width: Int) : BaseAdapter<Int, FloatColorHolder>(layoutId) {
  //当前桌面歌词的字体颜色 默认为当前主题颜色
  private var currentColor: Int
  private var itemWidth: Int

  /**
   * 判断是否是选中的颜色
   */
  private fun isColorChoose(context: Context, colorRes: Int): Boolean {
    return context.resources.getColor(colorRes) == currentColor
  }

  fun setCurrentColor(color: Int) {
    currentColor = color
    floatLyricTextColor = color
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloatColorHolder {
    val context = parent.context
    val holder = FloatColorHolder(
        LayoutInflater.from(context).inflate(R.layout.item_float_lrc_color, parent, false))
    val imgLayoutParam = RelativeLayout.LayoutParams(
        DensityUtil.dip2px(context, 18f), DensityUtil.dip2px(context, 18f))
    imgLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT)
    holder.binding.itemColor.layoutParams = imgLayoutParam
    val rootLayoutParam = RecyclerView.LayoutParams(itemWidth,
        ViewGroup.LayoutParams.MATCH_PARENT)
    holder.binding.root.layoutParams = rootLayoutParam
    return holder
  }

  override fun convert(holder: FloatColorHolder, colorRes: Int?, position: Int) {
    if (colorRes == null) {
      return
    }
    val color = if (colorRes != R.color.md_white_primary) ColorUtil.getColor(colorRes) else Color.parseColor("#F9F9F9")
    if (isColorChoose(holder.itemView.context, colorRes)) {
      holder.binding.itemColor.background = GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .strokeSize(DensityUtil.dip2px(1f))
          .strokeColor(Color.BLACK)
          .width(SIZE)
          .height(SIZE)
          .make()
    } else {
      holder.binding.itemColor.background = GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .width(SIZE)
          .height(SIZE)
          .make()
    }
    holder.binding.root.setOnClickListener { v: View -> onItemClickListener?.onItemClick(v, position) }
  }

  class FloatColorHolder(view: View) : BaseViewHolder(view) {
    val binding: ItemFloatLrcColorBinding = ItemFloatLrcColorBinding.bind(view)
  }

  companion object {
    private val SIZE = DensityUtil.dip2px(18f)
    val COLORS = listOf(
        R.color.md_red_primary, R.color.md_brown_primary, R.color.md_navy_primary,
        R.color.md_green_primary, R.color.md_yellow_primary, R.color.md_purple_primary,
        R.color.md_indigo_primary, R.color.md_plum_primary, R.color.md_blue_primary,
        R.color.md_white_primary, R.color.md_pink_primary
    )
  }

  init {
    setDataList(COLORS)
    itemWidth = width / COLORS.size
    //宽度太小
    if (itemWidth < DensityUtil.dip2px(context, 20f)) {
      itemWidth = DensityUtil.dip2px(context, 20f)
    }
    currentColor = floatLyricTextColor
  }
}