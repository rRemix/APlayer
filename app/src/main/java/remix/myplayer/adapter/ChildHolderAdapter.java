package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-24.
 */
public class ChildHolderAdapter extends BaseAdapter<ChildHolderAdapter.ViewHoler> {
    private ArrayList<MP3Item> mInfoList;
    private int mType;
    private String mArg;
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    public ChildHolderAdapter(Context context, int type, String arg,MultiChoice multiChoice){
        super(context);
        this.mContext = context;
        this.mType = type;
        this.mArg = arg;
        this.mMultiChoice = multiChoice;
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.RECTANGLE,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.RECTANGLE,ThemeStore.getSelectColor(),size,size);
    }

    public void setList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }


    @Override
    public ViewHoler onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHoler(LayoutInflater.from(mContext).inflate(R.layout.item_child_holder,null,false));
    }

    @Override
    public void onBindViewHolder(final ViewHoler holder, int position) {
        final MP3Item temp = mInfoList.get(position);
        if(temp == null || temp.getId() < 0 || temp.Title.equals(mContext.getString(R.string.song_lose_effect))) {
            holder.mTitle.setText(R.string.song_lose_effect);
            holder.mColumnView.setVisibility(View.INVISIBLE);
            holder.mButton.setVisibility(View.INVISIBLE);
        } else {
            holder.mColumnView.setVisibility(View.VISIBLE);
            holder.mButton.setVisibility(View.VISIBLE);
            //获得正在播放的歌曲
            final MP3Item currentMP3 = MusicService.getCurrentMP3();
            //判断该歌曲是否是正在播放的歌曲
            //如果是,高亮该歌曲，并显示动画
            if(currentMP3 != null){
                boolean highlight = temp.getId() == currentMP3.getId();
                holder.mTitle.setTextColor(highlight ?
                        ThemeStore.getAccentColor() :
                        ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
                holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.INVISIBLE);

                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
                if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && highlight){
                    holder.mColumnView.startAnim();
                }
                else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                    holder.mColumnView.stopAnim();
                }
            }
            //是否无损
            String prefix = temp.getDisplayname().substring(temp.getDisplayname().lastIndexOf(".") + 1);
            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);

            //设置标题
            holder.mTitle.setText(CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE));

            if(holder.mButton != null) {
                //设置按钮着色
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);

                //item点击效果
                holder.mButton.setBackground(Theme.getPressDrawable(
                        mDefaultDrawable,
                        mSelectDrawable,
                        ThemeStore.getRippleColor(),
                        null,null));

                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow())
                            return;
                        Intent intent = new Intent(mContext, OptionDialog.class);
                        intent.putExtra("MP3Item", temp);
                        if (mType == Constants.PLAYLIST) {
                            intent.putExtra("IsDeletePlayList", true);
                            intent.putExtra("PlayListName", mArg);
                        }
                        mContext.startActivity(intent);
                    }
                });
            }
        }

        //背景点击效果
        holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        if(holder.mContainer != null && mOnItemClickLitener != null){
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition());
                    return true;
                }
            });
        }

        if(MultiChoice.TAG.equals(ChildHolderActivity.TAG) &&
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            mMultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return mInfoList == null ? 0 : mInfoList.size();
    }

    static class ViewHoler extends BaseViewHolder {
        @BindView(R.id.sq)
        View mSQ;
        @BindView(R.id.album_holder_item_title)
        TextView mTitle;
        @BindView(R.id.song_item_button)
        public ImageButton mButton;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        public View mContainer;
        ViewHoler(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }
}
