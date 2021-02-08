package remix.myplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.databinding.ItemFolderRecycleBinding;
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
    holder.binding.folderName.setText(folder.getName());
    holder.binding.folderPath.setText(folder.getPath());
    holder.binding.folderNum.setText(String.format("%d首", folder.getCount()));
    //根据主题模式 设置图片
    holder.binding.folderImage.setImageDrawable(Theme
        .tintDrawable(context.getResources().getDrawable(R.drawable.icon_folder),
            ThemeStore.isLightTheme() ? Color.BLACK : Color.WHITE));

    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.binding.folderButton, R.drawable.icon_player_more, tintColor);

    holder.binding.folderButton.setOnClickListener(v -> {
      final PopupMenu popupMenu = new PopupMenu(context, holder.binding.folderButton);
      popupMenu.getMenuInflater().inflate(R.menu.menu_folder_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new LibraryListener(context,
          folder.getParentId(),
          Constants.FOLDER,
          folder.getPath()));
      popupMenu.setGravity(Gravity.END);
      popupMenu.show();
    });

    if (mOnItemClickListener != null) {
      holder.binding.getRoot().setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, position));
      holder.binding.getRoot().setOnLongClickListener(v -> {
        mOnItemClickListener.onItemLongClick(v, position);
        return true;
      });
    }

    holder.binding.getRoot().setSelected(mChoice.isPositionCheck(position));

  }


  static class FolderHolder extends BaseViewHolder {

    private final ItemFolderRecycleBinding binding;

    public FolderHolder(View itemView) {
      super(itemView);
      binding = ItemFolderRecycleBinding.bind(itemView);
    }
  }

}
