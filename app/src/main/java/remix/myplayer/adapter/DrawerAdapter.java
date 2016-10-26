package remix.myplayer.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnModeChangeListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/26 11:05
 */

public class DrawerAdapter extends BaseAdapter<DrawerAdapter.DrawerHolder>{
    //当前选中项
    public static int mSelectIndex = 0;
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

    @Override
    public void onBindViewHolder(DrawerHolder holder, final int position) {
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
                mOnItemClickLitener.onItemClick(v,position);
            }
        });
        holder.mRoot.setSelected(mSelectIndex == position);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#eff1f6")),
                    mContext.getResources().getDrawable(R.drawable.bg_list_default_day),null);
            stateListDrawable.addState(new int[]{android.R.attr.state_selected},
                    Theme.TintDrawable(mContext.getResources().getDrawable(R.drawable.bg_list_default_day),Color.parseColor("#eff1f6")));
            stateListDrawable.addState(new int[]{}, rippleDrawable);
            holder.mRoot.setBackground(stateListDrawable);
        }

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
