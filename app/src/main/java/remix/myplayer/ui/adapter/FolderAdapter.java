package remix.myplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.misc.menu.LibraryListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.util.Constants;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends BaseAdapter<Folder, FolderAdapter.FolderHolder> {

  private MultipleChoice<Folder> mChoice;

  public FolderAdapter(int layoutId, MultipleChoice<Folder> multiChoice) {
    super(layoutId);
    mChoice = multiChoice;
  }

  @Override
  public void onBindViewHolder(@NonNull FolderAdapter.FolderHolder holder, int position) {
    convert(holder, getItem(position), position);
  }

  @SuppressLint({"DefaultLocale", "RestrictedApi"})
  @Override
  protected void convert(final FolderHolder holder, Folder folder, int position) {
    final Context context = holder.itemView.getContext();
    //设置文件夹名字 路径名 歌曲数量
    holder.mName.setText(folder.getName());
    holder.mPath.setText(folder.getPath());
    holder.mCount.setText(String.format("%d首", folder.getCount()));
    //根据主题模式 设置图片
    if (holder.mImg != null) {
      holder.mImg.setImageDrawable(Theme
          .tintDrawable(context.getResources().getDrawable(R.drawable.icon_folder),
              ThemeStore.isLightTheme() ? Color.BLACK : Color.WHITE));
    }

    if (holder.mButton != null) {
      int tintColor = ThemeStore.getLibraryBtnColor();
      Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

      holder.mButton.setOnClickListener(v -> {
        final PopupMenu popupMenu = new PopupMenu(context, holder.mButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_folder_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new LibraryListener(context,
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
