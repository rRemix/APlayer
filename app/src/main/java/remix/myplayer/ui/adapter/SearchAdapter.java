package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import io.reactivex.disposables.Disposable;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ItemSearchReulstBinding;
import remix.myplayer.misc.menu.SongPopupListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;

/**
 * Created by Remix on 2016/1/23.
 */

/**
 * 搜索结果的适配器
 */
public class SearchAdapter extends BaseAdapter<Song, SearchAdapter.SearchResHolder> {

  public SearchAdapter(int layoutId) {
    super(layoutId);
  }

  @Override
  public void onViewRecycled(@NonNull SearchAdapter.SearchResHolder holder) {
    super.onViewRecycled(holder);
    if ((holder).binding.searchImage.getTag() != null) {
      Disposable disposable = (Disposable) (holder).binding.searchImage.getTag();
      if (!disposable.isDisposed()) {
        disposable.dispose();
      }
    }
    holder.binding.searchImage.setImageURI(Uri.EMPTY);
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(final SearchResHolder holder, Song song, int position) {
    holder.binding.searchName.setText(song.getTitle());
    holder.binding.searchDetail.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
    //封面
    Disposable disposable = new LibraryUriRequest(holder.binding.searchImage,
        getSearchRequestWithAlbumType(song),
        new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
    holder.binding.searchImage.setTag(disposable);

    //设置按钮着色
    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.binding.searchButton, R.drawable.icon_player_more, tintColor);

    holder.binding.searchButton.setOnClickListener(v -> {
      final PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), holder.binding.searchButton, Gravity.END);
      popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(
          new SongPopupListener((AppCompatActivity) holder.itemView.getContext(), song, false, ""));
      popupMenu.show();
    });

    if (mOnItemClickListener != null) {
      holder.binding.reslistItem.setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, holder.getAdapterPosition()));
    }
  }

  static class SearchResHolder extends BaseViewHolder {

    private final ItemSearchReulstBinding binding;

    SearchResHolder(View itemView) {
      super(itemView);
      binding = ItemSearchReulstBinding.bind(itemView);
    }
  }
}
