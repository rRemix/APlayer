package remix.myplayer.ui.adapter;

import static remix.myplayer.theme.ThemeStore.getDrawerDefaultColor;
import static remix.myplayer.theme.ThemeStore.getDrawerEffectColor;

import android.view.View;
import remix.myplayer.R;
import remix.myplayer.databinding.ItemDrawerBinding;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/26 11:05
 */

public class DrawerAdapter extends BaseAdapter<Integer, DrawerAdapter.DrawerHolder> {

  //当前选中项
  private int mSelectIndex = 0;
  private int[] IMAGES = new int[]{R.drawable.drawer_icon_musicbox,
      R.drawable.ic_history_24dp,
      R.drawable.drawer_icon_recently_add,
      R.drawable.darwer_icon_support,
      R.drawable.darwer_icon_set,
      R.drawable.drawer_icon_exit};
  private int[] TITLES = new int[]{R.string.drawer_song,
      R.string.drawer_history,
      R.string.drawer_recently_add,
      R.string.support_develop,
      R.string.drawer_setting,
      R.string.exit};

  public DrawerAdapter(int layoutId) {
    super(layoutId);
  }

  public void setSelectIndex(int index) {
    mSelectIndex = index;
    notifyDataSetChanged();
  }

  @Override
  protected Integer getItem(int position) {
    return TITLES[position];
  }

  @Override
  protected void convert(final DrawerHolder holder, Integer titleRes, int position) {
    Theme.tintDrawable(holder.binding.itemImg, IMAGES[position], ThemeStore.getAccentColor());

    holder.binding.itemText.setText(titleRes);
    holder.binding.itemText
        .setTextColor(Theme.resolveColor(holder.itemView.getContext(), R.attr.text_color_primary));
    holder.binding.itemRoot
        .setOnClickListener(v -> mOnItemClickListener.onItemClick(v, position));
    holder.binding.itemRoot.setSelected(mSelectIndex == position);
    holder.binding.itemRoot.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(
        holder.itemView.getContext(),
        new GradientDrawableMaker()
            .color(getDrawerEffectColor()).make(),
        new GradientDrawableMaker()
            .color(getDrawerDefaultColor()).make(),
        getDrawerEffectColor()));

  }

  @Override
  public int getItemCount() {
    return TITLES != null ? TITLES.length : 0;
  }

  static class DrawerHolder extends BaseViewHolder {

    private final ItemDrawerBinding binding;

    DrawerHolder(View itemView) {
      super(itemView);
      binding = ItemDrawerBinding.bind(itemView);
    }
  }
}
