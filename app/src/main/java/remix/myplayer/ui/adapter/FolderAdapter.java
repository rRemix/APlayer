package remix.myplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.misc.menu.LibraryListener;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends BaseAdapter<Folder, FolderAdapter.FolderHolder> {

  private Drawable mDefaultDrawable;
  private Drawable mSelectDrawable;
  private MultipleChoice<Folder> mChoice;

  public FolderAdapter(Context context, int layoutId, MultipleChoice<Folder> multiChoice) {
    super(context, layoutId);
    int size = DensityUtil.dip2px(mContext, 45);
    mChoice = multiChoice;

    mDefaultDrawable = new GradientDrawableMaker()
        .color(Color.TRANSPARENT)
        .width(size)
        .height(size)
        .make();
    mSelectDrawable = new GradientDrawableMaker()
        .color(ThemeStore.getSelectColor())
        .width(size)
        .height(size)
        .make();
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
      holder.mImg.setImageDrawable(Theme
          .tintDrawable(mContext.getResources().getDrawable(R.drawable.icon_folder),
              ThemeStore.isLightTheme() ? Color.BLACK : Color.WHITE));
    }

    if (holder.mButton != null) {
      int tintColor = ThemeStore.getLibraryBtnColor();
      Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

      holder.mButton.setOnClickListener(v -> {
        final PopupMenu popupMenu = new PopupMenu(mContext, holder.mButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_folder_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new LibraryListener(mContext,
            folder.getParentId(),
            Constants.FOLDER,
            folder.getPath()));
        popupMenu.setGravity(Gravity.END);
        popupMenu.show();
      });
    }

    if (mOnItemClickListener != null && holder.mContainer != null) {
      holder.mContainer.setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, position));
      holder.mContainer.setOnLongClickListener(v -> {
        mOnItemClickListener.onItemLongClick(v, position);
        return true;
      });
    }

    holder.mContainer.setSelected(mChoice.isPositionCheck(position));

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
