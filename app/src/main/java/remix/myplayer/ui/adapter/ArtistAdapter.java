package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.misc.menu.LibraryListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.adapter.holder.HeaderHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家界面的适配器
 */
public class ArtistAdapter extends HeaderAdapter<Artist, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  public ArtistAdapter(int layoutId, MultipleChoice multiChoice,
      FastScrollRecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView);
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      return new HeaderHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_header_2, parent, false));
    }
    return viewType == HeaderAdapter.LIST_MODE ?
        new ArtistAdapter.ArtistListHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_recycle_list, parent, false)) :
        new ArtistAdapter.ArtistGridHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_recycle_grid, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint({"RestrictedApi", "CheckResult"})
  @Override
  protected void convert(final BaseViewHolder baseHolder, final Artist artist, final int position) {
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      setUpModeButton(headerHolder);
      return;
    }

    if (!(baseHolder instanceof ArtistHolder)) {
      return;
    }

    final Context context = baseHolder.itemView.getContext();
    final ArtistHolder holder = (ArtistHolder) baseHolder;
    //设置歌手名
    holder.mText1.setText(artist.getArtist());
    final long artistId = artist.getArtistID();
    if (holder instanceof ArtistListHolder && holder.mText2 != null) {
      if (artist.getCount() > 0) {
        holder.mText2.setText(context.getString(R.string.song_count_1, artist.getCount()));
      } else {
        holder.mText2.setText(App.getContext().getString(R.string.song_count_1, artist.getCount()));
      }
    }
    //设置封面
    final int imageSize = mMode == LIST_MODE ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;
    holder.mImage.setTag(setImage(holder.mImage, ImageUriUtil.getSearchRequest(artist), imageSize, position));

    holder.mContainer.setOnClickListener(v -> {
      if (holder.getAdapterPosition() - 1 < 0) {
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

    //popupmenu
    int tintColor = ThemeStore.getLibraryBtnColor();

    Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(context, holder.mButton);
      popupMenu.getMenuInflater().inflate(R.menu.menu_artist_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new LibraryListener(context,
          artistId,
          Constants.ARTIST,
          artist.getArtist()));
      popupMenu.setGravity(Gravity.END);
      popupMenu.show();
    });

    //是否处于选中状态
    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));

    //设置padding
    setMarginForGridLayout(holder, position);
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && position - 1 < mDatas.size()) {
      String artist = mDatas.get(position - 1).getArtist();
      return !TextUtils.isEmpty(artist) ? (Pinyin.toPinyin(artist.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  static class ArtistHolder extends BaseViewHolder {

    @BindView(R.id.item_text1)
    TextView mText1;
    @BindView(R.id.item_text2)
    @Nullable
    TextView mText2;
    @BindView(R.id.item_simpleiview)
    SimpleDraweeView mImage;
    @BindView(R.id.item_button)
    ImageButton mButton;
    @BindView(R.id.item_container)
    ViewGroup mContainer;

    ArtistHolder(View v) {
      super(v);
    }
  }

  static class ArtistListHolder extends ArtistHolder {

    ArtistListHolder(View v) {
      super(v);
    }
  }

  static class ArtistGridHolder extends ArtistHolder {

    ArtistGridHolder(View v) {
      super(v);
    }
  }


}
