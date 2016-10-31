package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.util.DialogUtils;

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

public class DrawerAdapter extends BaseAdapter<DrawerAdapter.DrawerHolder>{
    //当前选中项
    public int mSelectIndex = 0;
    private int[] mImgs = new int[]{R.drawable.drawer_icon_musicbox,R.drawable.darwer_icon_folder,
                                    R.drawable.darwer_icon_night,R.drawable.darwer_icon_set};
    private String[] mTitles = new String[]{"歌曲库","文件夹","夜间模式","设置"};
    public DrawerAdapter(Context Context) {
        super(Context);
    }
    private OnModeChangeListener mModeChangeListener;

    public void setOnModeChangeListener(OnModeChangeListener l){
        mModeChangeListener = l;
    }

    @Override
    public DrawerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DrawerHolder(LayoutInflater.from(mContext).inflate(R.layout.item_drawer,parent,false));
    }

    public void setSelectIndex(int index){
        mSelectIndex = index;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final DrawerHolder holder, final int position) {
//        holder.mImg.setImageDrawable(Theme.getPressAndSelectedStateListDrawalbe(mContext,mImgs[position]));
        holder.mImg.setImageResource(mImgs[position]);
        holder.mText.setText(mTitles[position]);
        if(position == 2){
            holder.mSwitch.setVisibility(View.VISIBLE);
            holder.mSwitch.setChecked(!ThemeStore.isDay());
            holder.mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(mModeChangeListener != null)
                        mModeChangeListener.OnModeChange(isChecked);
                }
            });
        }
        holder.mRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
            }
        });
        holder.mRoot.setSelected(mSelectIndex == position);
        holder.mRoot.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawalbe(
                        mContext,
                        R.drawable.bg_list_default_day,
                        ColorUtil.getColor(R.color.drawer_selected)));

    }

    @Override
    public int getItemCount() {
        return 4;
    }

    class DrawerHolder extends BaseViewHolder{
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
