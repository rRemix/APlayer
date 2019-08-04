package remix.myplayer.ui.adapter;

import static remix.myplayer.theme.ThemeStore.getDrawerDefaultColor;
import static remix.myplayer.theme.ThemeStore.getDrawerEffectColor;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import remix.myplayer.R;
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
  private int[] IMAGES = new int[]{R.drawable.drawer_icon_musicbox, R.drawable.drawer_icon_recently,
      R.drawable.darwer_icon_support, R.drawable.darwer_icon_set, R.drawable.drawer_icon_exit};
  private int[] TITLES = new int[]{R.string.drawer_song, R.string.drawer_recently,
      R.string.support_develop, R.string.drawer_setting, R.string.exit};

  public DrawerAdapter(Context Context, int layoutId) {
    super(Context, layoutId);
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
    Theme.tintDrawable(holder.mImg, IMAGES[position], ThemeStore.getAccentColor());

    holder.mText.setText(titleRes);
    holder.mText.setTextColor(Theme.resolveColor(mContext, R.attr.text_color_primary));
    holder.mRoot
        .setOnClickListener(v -> mOnItemClickListener.onItemClick(v, position));
    holder.mRoot.setSelected(mSelectIndex == position);
    holder.mRoot.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(
        mContext,
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

    @BindView(R.id.item_img)
    ImageView mImg;
    @BindView(R.id.item_text)
    TextView mText;
    @BindView(R.id.item_root)
    RelativeLayout mRoot;

    DrawerHolder(View itemView) {
      super(itemView);
    }
  }
}
