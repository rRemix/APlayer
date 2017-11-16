package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.FolderActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends BaseAdapter<String,FolderAdapter.FolderHolder> {
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    public FolderAdapter(Context context,int layoutId,MultiChoice multiChoice) {
        super(context,layoutId);
        this.mMultiChoice = multiChoice;
        int size = DensityUtil.dip2px(mContext,45);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    @Override
    protected String getItem(int position) {
        if(Global.FolderMap == null || Global.FolderMap.size() < 0)
            return "";
        Iterator it = Global.FolderMap.keySet().iterator();
        String name = "";
        for(int i = 0 ; i <= position ; i++) {
            if(it.hasNext())
                name = (String) it.next();
        }
        return name;
    }

    @Override
    protected void convert(final FolderHolder holder, String folderName, int position) {
        if(Global.FolderMap == null || Global.FolderMap.size() < 0)
            return ;
        if(TextUtils.isEmpty(folderName))
            return;
        //设置文件夹名字 路径名 歌曲数量
        holder.mName.setText(folderName.substring(folderName.lastIndexOf("/") + 1,folderName.length()));
        holder.mPath.setText(folderName);
        if(Global.FolderMap.get(folderName) != null)
            holder.mCount.setText(String.format("%d首", Global.FolderMap.get(folderName).size()));
        //根据主题模式 设置图片
        if(holder.mImg != null) {
            holder.mImg.setImageDrawable(Theme.TintDrawable(mContext.getResources().getDrawable(R.drawable.icon_folder),ThemeStore.isDay() ? Color.BLACK : Color.WHITE));
//            holder.mImg.setImageResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.icon_folder_day : R.drawable.icon_folder);
        }

        //背景点击效果
        holder.mContainer.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        final String full_path = folderName;
        if(holder.mButton != null) {
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

            //item点击效果
            holder.mButton.setBackground(Theme.getPressDrawable(
                    mDefaultDrawable,
                    mSelectDrawable,
                    ThemeStore.getRippleColor(),
                    null,null));

            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                    final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                    popupMenu.getMenuInflater().inflate(R.menu.folder_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                            holder.getAdapterPosition(),
                            Constants.FOLDER,
                            full_path));
                    popupMenu.setGravity(Gravity.END);
                    popupMenu.show();
                }
            });
        }

        if(mOnItemClickLitener != null && holder.mContainer != null) {
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

        if(MultiChoice.TAG.equals(FolderActivity.TAG) &&
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            mMultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return Global.FolderMap == null ? 0 : Global.FolderMap.size();
    }

    static class FolderHolder extends BaseViewHolder {
        public View mContainer;
        @BindView(R.id.folder_image)
        public ImageView mImg;
        @BindView(R.id.folder_name)
        public TextView mName;
        @BindView(R.id.folder_path)
        public TextView mPath;
        @BindView(R.id.folder_num)
        public TextView mCount;
        @BindView(R.id.folder_button)
        public ImageButton mButton;
        public FolderHolder(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }

}
