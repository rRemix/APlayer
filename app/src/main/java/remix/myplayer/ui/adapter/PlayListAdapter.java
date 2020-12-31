package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.R;
import remix.myplayer.db.room.model.PlayList;
import remix.myplayer.misc.menu.LibraryListener;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.PlayListUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.UriRequest;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.adapter.holder.HeaderHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends HeaderAdapter<PlayList, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  public PlayListAdapter(int layoutId, MultipleChoice multiChoice,
      RecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView);
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      return new HeaderHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.layout_header_2, parent, false));
    }
    return viewType == HeaderAdapter.LIST_MODE ?
        new PlayListListHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_recycle_list, parent, false)) :
        new PlayListGridHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_recycle_grid, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(BaseViewHolder baseHolder, final PlayList info, int position) {
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      setUpModeButton(headerHolder);
      return;
    }

    if (!(baseHolder instanceof PlayListHolder)) {
      return;
    }
    final PlayListHolder holder = (PlayListHolder) baseHolder;
    if (info == null) {
      return;
    }

    final Context context = baseHolder.itemView.getContext();
    holder.mName.setText(info.getName());
    holder.mOther.setText(context.getString(R.string.song_count, info.getAudioIds().size()));

    //设置专辑封面
    final int imageSize = mMode == LIST_MODE ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;

    new PlayListUriRequest(holder.mImage,
        new UriRequest(info.getId(), ImageUriRequest.URL_PLAYLIST, UriRequest.TYPE_NETEASE_SONG),
        new RequestConfig.Builder(imageSize, imageSize).build()) {
    }.load();

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

    Theme.tintDrawable(holder.mButton,
        R.drawable.icon_player_more,
        ThemeStore.getLibraryBtnColor());

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(context, holder.mButton);
      popupMenu.getMenuInflater().inflate(R.menu.menu_playlist_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(
          new LibraryListener(context, info.getId(), Constants.PLAYLIST, info.getName()));
      popupMenu.show();
    });

    //是否处于选中状态
    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));

    setMarginForGridLayout(holder, position);
  }


  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && position - 1 < mDatas.size()) {
      String title = mDatas.get(position - 1).getName();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  static class PlayListHolder extends BaseViewHolder {

    @BindView(R.id.item_text1)
    TextView mName;
    @BindView(R.id.item_text2)
    TextView mOther;
    @BindView(R.id.item_simpleiview)
    SimpleDraweeView mImage;
    @BindView(R.id.item_button)
    ImageView mButton;
    @BindView(R.id.item_container)
    ViewGroup mContainer;

    PlayListHolder(View itemView) {
      super(itemView);
    }
  }

  static class PlayListListHolder extends PlayListHolder {

    PlayListListHolder(View itemView) {
      super(itemView);
    }
  }

  static class PlayListGridHolder extends PlayListHolder {

    PlayListGridHolder(View itemView) {
      super(itemView);
    }
  }

}
