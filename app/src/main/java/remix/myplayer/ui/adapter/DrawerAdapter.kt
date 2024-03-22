package remix.myplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import remix.myplayer.R
import remix.myplayer.databinding.ItemDrawerBinding
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.theme.ThemeStore.drawerDefaultColor
import remix.myplayer.theme.ThemeStore.drawerEffectColor
import remix.myplayer.ui.adapter.DrawerAdapter.DrawerHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/26 11:05
 */
class DrawerAdapter(layoutId: Int) : BaseAdapter<Int, DrawerHolder>(layoutId) {
  //当前选中项
  private var selectIndex = 0

  private val IMAGES = intArrayOf(R.drawable.drawer_icon_musicbox,
      R.drawable.ic_history_24dp,
      R.drawable.drawer_icon_recently_add,
      R.drawable.darwer_icon_support,
      R.drawable.darwer_icon_set,
      R.drawable.drawer_icon_exit)
  private val TITLES: IntArray = intArrayOf(R.string.drawer_song,
      R.string.drawer_history,
      R.string.drawer_recently_add,
      R.string.support_develop,
      R.string.drawer_setting,
      R.string.exit)

  fun setSelectIndex(index: Int) {
    selectIndex = index
    notifyDataSetChanged()
  }

  override fun getItem(position: Int): Int {
    return TITLES[position]
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerHolder {
    return DrawerHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false))
  }

  override fun convert(holder: DrawerHolder, titleRes: Int?, position: Int) {
    if(titleRes == null){
      return
    }
    Theme.tintDrawable(holder.binding.itemImg, IMAGES[position], accentColor)
    holder.binding.itemText.setText(titleRes)
    holder.binding.itemText
        .setTextColor(Theme.resolveColor(holder.itemView.context, R.attr.text_color_primary))
    holder.binding.itemRoot
        .setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, position) }
    holder.binding.itemRoot.isSelected = selectIndex == position
    holder.binding.itemRoot.background = Theme.getPressAndSelectedStateListRippleDrawable(
        holder.itemView.context,
        GradientDrawableMaker()
            .color(drawerEffectColor).make(),
        GradientDrawableMaker()
            .color(drawerDefaultColor).make(),
        drawerEffectColor)
  }

  override fun getItemCount(): Int {
    return TITLES.size
  }

  class DrawerHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemDrawerBinding = ItemDrawerBinding.bind(itemView)

  }
}