package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class FolderAdapter extends BaseAdapter<FolderAdapter.FolderHolder> {
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    public FolderAdapter(Context context,MultiChoice multiChoice) {
        super(context);
        this.mMultiChoice = multiChoice;
        int size = DensityUtil.dip2px(mContext,45);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    @Override
    public FolderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderHolder(LayoutInflater.from(mContext).inflate(R.layout.item_folder_recycle,null,false));
    }

    @Override
    public void onBindViewHolder(final FolderHolder holder, final int position) {
        if(Global.mFolderMap == null || Global.mFolderMap.size() < 0)
            return ;
        //根据当前索引 获得对应的文件夹名字
        Iterator it = Global.mFolderMap.keySet().iterator();
        String temp = null;
        for(int i = 0 ; i <= position ; i++) {
            if(it.hasNext())
                temp = it.next().toString();
        }
        //设置文件夹名字 路径名 歌曲数量
        if(temp != null){
            holder.mName.setText(temp.substring(temp.lastIndexOf("/")+ 1,temp.length()));
            holder.mPath.setText(temp);
            if(Global.mFolderMap.get(temp) != null)
                holder.mCount.setText(Global.mFolderMap.get(temp).size()+ "首");
        }
        //根据主题模式 设置图片
        if(holder.mImg != null) {
            holder.mImg.setImageDrawable(Theme.TintDrawable(mContext.getResources().getDrawable(R.drawable.icon_folder),ThemeStore.isDay() ? Color.BLACK : Color.WHITE));
//            holder.mImg.setImageResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.icon_folder_day : R.drawable.icon_folder);
        }

        //背景点击效果
        holder.mContainer.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        final String full_path = temp;
        if(holder.mButton != null) {
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
        return Global.mFolderMap == null ? 0 : Global.mFolderMap.size();
    }

    public static class FolderHolder extends BaseViewHolder {
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
