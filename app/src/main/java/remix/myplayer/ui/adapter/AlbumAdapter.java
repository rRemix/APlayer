package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.databinding.ItemAlbumRecycleGridBinding;
import remix.myplayer.databinding.ItemAlbumRecycleListBinding;
import remix.myplayer.misc.menu.LibraryListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.adapter.holder.HeaderHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdapter extends HeaderAdapter<Album, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  public AlbumAdapter(int layoutId, MultipleChoice multipleChoice,
      RecyclerView recyclerView) {
    super(layoutId, multipleChoice, recyclerView);
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      return new HeaderHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_header_2, parent, false));
    }
    return viewType == HeaderAdapter.LIST_MODE ?
        new AlbumListHolder(ItemAlbumRecycleListBinding
            .inflate(LayoutInflater.from(parent.getContext()), parent, false)) :
        new AlbumGridHolder(ItemAlbumRecycleGridBinding
            .inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(BaseViewHolder baseHolder, Album album, int position) {
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      setUpModeButton(headerHolder);
      return;
    }

    if (!(baseHolder instanceof AlbumHolder)) {
      return;
    }

    final Context context = baseHolder.itemView.getContext();
    final AlbumHolder holder = (AlbumHolder) baseHolder;
    holder.mText1.setText(album.getAlbum());

    //设置封面
    final long albumId = album.getAlbumID();
    final int imageSize = mMode == LIST_MODE ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;
    holder.mImage.setTag(setImage(holder.mImage, ImageUriUtil.getSearchRequest(album), imageSize, position));

    if (holder instanceof AlbumListHolder) {
      holder.mText2
          .setText(App.getContext().getString(R.string.song_count_2, album.getArtist(), album.getCount()));
    } else {
      holder.mText2.setText(album.getArtist());
    }

    holder.mContainer.setOnClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(holder.mContainer, position - 1);
    });
    //多选菜单
    holder.mContainer.setOnLongClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemLongClick(holder.mContainer, position - 1);
      return true;
    });

    //着色
    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(context, holder.mButton, Gravity.END);
      popupMenu.getMenuInflater().inflate(R.menu.menu_album_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new LibraryListener(
          context,
          albumId,
          Constants.ALBUM,
          album.getAlbum()));
      popupMenu.show();
    });

    //是否处于选中状态
    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));

    //半圆着色
    if (mMode == HeaderAdapter.GRID_MODE) {
      Theme.tintDrawable(holder.mHalfCircle, R.drawable.icon_half_circular_left,
          ThemeStore.getBackgroundColorMain(context));
    }

    setMarginForGridLayout(holder, position);
  }


  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && position - 1 < mDatas.size()) {
      String album = mDatas.get(position - 1).getAlbum();
      return !TextUtils.isEmpty(album) ? (Pinyin.toPinyin(album.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  static class AlbumHolder extends BaseViewHolder {

    @Nullable
    ImageView mHalfCircle;
    TextView mText1;
    TextView mText2;
    ImageButton mButton;
    SimpleDraweeView mImage;
    ViewGroup mContainer;

    AlbumHolder(View v) {
      super(v);
    }
  }

  static class AlbumGridHolder extends AlbumHolder {

    AlbumGridHolder(ItemAlbumRecycleGridBinding binding) {
      super(binding.getRoot());
      mHalfCircle = binding.itemHalfCircle;
      mText1 = binding.itemText1;
      mText2 = binding.itemText2;
      mButton = binding.itemButton;
      mImage = binding.itemSimpleiview;
      mContainer = binding.itemContainer;
    }
  }

  static class AlbumListHolder extends AlbumHolder {

    AlbumListHolder(ItemAlbumRecycleListBinding binding) {
      super(binding.getRoot());
      mText1 = binding.itemText1;
      mText2 = binding.itemText2;
      mButton = binding.itemButton;
      mImage = binding.itemSimpleiview;
      mContainer = binding.itemContainer;
    }
  }

}
