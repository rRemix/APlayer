package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
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

  public AlbumAdapter(Context context, int layoutId, MultipleChoice multipleChoice,
      RecyclerView recyclerView) {
    super(context, layoutId, multipleChoice, recyclerView);
  }

  @Override
  public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      return new HeaderHolder(
          LayoutInflater.from(mContext).inflate(R.layout.layout_header_2, parent, false));
    }
    return viewType == HeaderAdapter.LIST_MODE ?
        new AlbumListHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_album_recycle_list, parent, false)) :
        new AlbumGridHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_album_recycle_grid, parent, false));
  }

  @Override
  public void onViewRecycled(BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
//    if (holder instanceof AlbumHolder) {
//      final AlbumHolder albumHolder = (AlbumHolder) holder;
//      if (albumHolder.mImage.getTag() != null) {
//        Disposable disposable = (Disposable) albumHolder.mImage.getTag();
//        if (!disposable.isDisposed()) {
//          disposable.dispose();
//        }
//      }
//      albumHolder.mImage.setImageURI(Uri.EMPTY);
//    }
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
    final AlbumHolder holder = (AlbumHolder) baseHolder;
    holder.mText1.setText(album.getAlbum());

    //设置封面
    final int albumId = album.getAlbumID();
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
        ToastUtil.show(mContext, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(holder.mContainer, position - 1);
    });
    //多选菜单
    holder.mContainer.setOnLongClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(mContext, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemClick(holder.mContainer, position - 1);
      return true;
    });

    //着色
    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(mContext, holder.mButton, Gravity.END);
      popupMenu.getMenuInflater().inflate(R.menu.menu_album_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new LibraryListener(mContext,
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
          ThemeStore.getBackgroundColorMain(mContext));
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

    @BindView(R.id.item_half_circle)
    @Nullable
    ImageView mHalfCircle;
    @BindView(R.id.item_text1)
    TextView mText1;
    @BindView(R.id.item_text2)
    TextView mText2;
    @BindView(R.id.item_button)
    ImageButton mButton;
    @BindView(R.id.item_simpleiview)
    SimpleDraweeView mImage;
    @BindView(R.id.item_container)
    RelativeLayout mContainer;
//        @BindView(R.id.item_root)
//        @Nullable
//        View mRoot;

    AlbumHolder(View v) {
      super(v);
    }
  }

  static class AlbumGridHolder extends AlbumHolder {

    AlbumGridHolder(View v) {
      super(v);
    }
  }

  static class AlbumListHolder extends AlbumHolder {

    AlbumListHolder(View v) {
      super(v);
    }
  }

}
