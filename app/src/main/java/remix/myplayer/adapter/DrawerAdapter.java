package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnModeChangeListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/26 11:05
 */

public class DrawerAdapter extends BaseAdapter<Integer,DrawerAdapter.DrawerHolder>{
    //当前选中项
    private int mSelectIndex = 0;
    private int[] IMAGES = new int[]{R.drawable.drawer_icon_musicbox,R.drawable.drawer_icon_recently,R.drawable.darwer_icon_folder,
                                    R.drawable.darwer_icon_night,R.drawable.darwer_icon_set};
    private int[] TITLES = new int[]{R.string.drawer_song,R.string.drawer_recently,
                                    R.string.drawer_folder,R.string.drawer_night,R.string.drawer_setting};
    public DrawerAdapter(Context Context,int layoutId) {
        super(Context,layoutId);
    }
    private OnModeChangeListener mModeChangeListener;

    public void setOnModeChangeListener(OnModeChangeListener l){
        mModeChangeListener = l;
    }

    public void setSelectIndex(int index){
        mSelectIndex = index;
        notifyDataSetChanged();
    }

    @Override
    protected Integer getItem(int position) {
        return position;
    }

    @Override
    protected void convert(final DrawerHolder holder, Integer item, int position) {
        Theme.TintDrawable(holder.mImg, IMAGES[position],ThemeStore.getAccentColor());
        holder.mText.setText(TITLES[position]);
        holder.mText.setTextColor(ThemeStore.isDay() ? ColorUtil.getColor(R.color.gray_34353a) : ThemeStore.getTextColorPrimary());
        holder.mText.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_34353a : R.color.white_e5e5e5));
        if(position == 3){
            holder.mSwitch.setVisibility(View.VISIBLE);
            holder.mSwitch.setChecked(!ThemeStore.isDay());
            holder.mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(mModeChangeListener != null)
                    mModeChangeListener.OnModeChange(isChecked);
            });
        }
        holder.mRoot.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition()));
        holder.mRoot.setSelected(mSelectIndex == position);
        holder.mRoot.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(mContext,
                Theme.getShape(GradientDrawable.RECTANGLE, ThemeStore.getDrawerEffectColor()),
                Theme.getShape(GradientDrawable.RECTANGLE, ThemeStore.getDrawerDefaultColor()),
                ThemeStore.getDrawerEffectColor()));
    }

    @Override
    public int getItemCount() {
        return TITLES != null ? TITLES.length : 0;
    }

    static class DrawerHolder extends BaseViewHolder{
        @BindView(R.id.item_img)
        ImageView mImg;
        @BindView(R.id.item_text)
        TextView mText;
        @BindView(R.id.item_switch)
        SwitchCompat mSwitch;
        @BindView(R.id.item_root)
        RelativeLayout mRoot;
        DrawerHolder(View itemView) {
            super(itemView);
        }
    }
}
