package remix.myplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.misc.menu.AlbArtFolderPlaylistListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends BaseAdapter<Folder, FolderAdapter.FolderHolder> {
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;

    public FolderAdapter(Context context, int layoutId, MultiChoice multiChoice) {
        super(context, layoutId);
        this.mMultiChoice = multiChoice;
        int size = DensityUtil.dip2px(mContext, 45);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL, Color.TRANSPARENT, size, size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL, ThemeStore.getSelectColor(), size, size);
    }

    @Override
    public void onBindViewHolder(FolderAdapter.FolderHolder holder, int position) {
        convert(holder, getItem(position), position);
    }

    @SuppressLint({"DefaultLocale", "RestrictedApi"})
    @Override
    protected void convert(final FolderHolder holder, Folder folder, int position) {
        //设置文件夹名字 路径名 歌曲数量
        holder.mName.setText(folder.getName());
        holder.mPath.setText(folder.getPath());
        holder.mCount.setText(String.format("%d首", folder.getCount()));
        //根据主题模式 设置图片
        if (holder.mImg != null) {
            holder.mImg.setImageDrawable(Theme.TintDrawable(mContext.getResources().getDrawable(R.drawable.icon_folder), ThemeStore.isDay() ? Color.BLACK : Color.WHITE));
        }

        //背景点击效果
        holder.mContainer.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL, mContext));

        if (holder.mButton != null) {
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

            //item点击效果
            holder.mButton.setBackground(Theme.getPressDrawable(
                    mDefaultDrawable,
                    mSelectDrawable,
                    ThemeStore.getRippleColor(),
                    null, null));

            holder.mButton.setOnClickListener(v -> {
                Context wrapper = new ContextThemeWrapper(mContext, Theme.getPopupMenuStyle());
                final PopupMenu popupMenu = new PopupMenu(wrapper, holder.mButton);
                popupMenu.getMenuInflater().inflate(R.menu.menu_folder_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                        folder.getParentId(),
                        Constants.FOLDER,
                        folder.getPath()));
                popupMenu.setGravity(Gravity.END);
                popupMenu.show();
            });
        }

        if (mOnItemClickLitener != null && holder.mContainer != null) {
            holder.mContainer.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition()));
            holder.mContainer.setOnLongClickListener(v -> {
                mOnItemClickLitener.onItemLongClick(v, holder.getAdapterPosition());
                return true;
            });
        }

        if (mMultiChoice.getTag().equals(FolderFragment.TAG) &&
                mMultiChoice.getSelectPos().contains(position)) {
            holder.mContainer.setSelected(true);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

//    @Override
//    public int getItemCount() {
//        return Global.FolderMap == null ? 0 : Global.FolderMap.size();
//    }

    static class FolderHolder extends BaseViewHolder {
        View mContainer;
        @BindView(R.id.folder_image)
        ImageView mImg;
        @BindView(R.id.folder_name)
        TextView mName;
        @BindView(R.id.folder_path)
        TextView mPath;
        @BindView(R.id.folder_num)
        TextView mCount;
        @BindView(R.id.folder_button)
        ImageButton mButton;

        public FolderHolder(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }

}
