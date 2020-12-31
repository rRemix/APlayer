package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.theme.ThemeStore.getHighLightTextColor;
import static remix.myplayer.theme.ThemeStore.getTextColorPrimary;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.menu.SongPopupListener;
import remix.myplayer.service.Command;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.ToastUtil;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends HeaderAdapter<Song, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  private Song mLastPlaySong = MusicServiceRemote.getCurrentSong();

  public SongAdapter(int layoutId, MultipleChoice multiChoice, RecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView);
    mRecyclerView = recyclerView;
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return viewType == TYPE_HEADER ?
        new HeaderHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_header_1, parent, false)) :
        new SongViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_recycle, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(BaseViewHolder baseHolder, final Song song, int position) {
    final Context context = baseHolder.itemView.getContext();
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      //没有歌曲时隐藏
      if (mDatas == null || mDatas.size() == 0) {
        headerHolder.mRoot.setVisibility(View.GONE);
        return;
      } else {
        headerHolder.mRoot.setVisibility(View.VISIBLE);
      }

      headerHolder.mIvShuffle.setImageDrawable(
          Theme.tintVectorDrawable(context, R.drawable.ic_shuffle_white_24dp,
              ThemeStore.getAccentColor())
      );
      headerHolder.mTvShuffle.setText(context.getString(R.string.play_random, getItemCount() - 1));

      headerHolder.mRoot.setOnClickListener(v -> {
        Intent intent = MusicUtil.makeCmdIntent(Command.NEXT, true);
        if (mDatas == null || mDatas.isEmpty()) {
          ToastUtil.show(context, R.string.no_song);
          return;
        }
        MusicServiceRemote.setPlayQueue(mDatas, intent);
      });
      return;
    }

    if (!(baseHolder instanceof SongViewHolder)) {
      return;
    }
    final SongViewHolder holder = (SongViewHolder) baseHolder;

    //封面
    holder.mImage.setTag(
        setImage(holder.mImage, getSearchRequestWithAlbumType(song), SMALL_IMAGE_SIZE, position));

//        //是否为无损
//        if(!TextUtils.isEmpty(song.getDisplayName())){
//            String prefix = song.getDisplayName().substring(song.getDisplayName().lastIndexOf(".") + 1);
//            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
//        }

    //高亮
    if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
      mLastPlaySong = song;
      holder.mName.setTextColor(getHighLightTextColor());
      holder.mIndicator.setVisibility(View.VISIBLE);
    } else {
      holder.mName.setTextColor(getTextColorPrimary());
      holder.mIndicator.setVisibility(View.GONE);
    }
    holder.mIndicator.setBackgroundColor(getHighLightTextColor());

    //标题
    holder.mName.setText(song.getShowName());

    //艺术家与专辑
    holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

    //设置按钮着色
    int tintColor = ThemeStore.getLibraryBtnColor();
    Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

    holder.mButton.setOnClickListener(v -> {
      if (mChoice.isActive()) {
        return;
      }
      final PopupMenu popupMenu = new PopupMenu(context, holder.mButton, Gravity.END);
      popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(
          new SongPopupListener((AppCompatActivity) context, song, false, ""));
      popupMenu.show();
    });

    holder.mContainer.setOnClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(v, position - 1);
    });
    holder.mContainer.setOnLongClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemLongClick(v, position - 1);
      return true;
    });

    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && position - 1 < mDatas.size()) {
      String title = mDatas.get(position - 1).getTitle();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  /**
   * 更新高亮歌曲
   */
  public void updatePlayingSong() {
    final Song currentSong = MusicServiceRemote.getCurrentSong();
    if (currentSong.getId() == -1 || currentSong.getId() == mLastPlaySong.getId()) {
      return;
    }

    if (mDatas != null && mDatas.indexOf(currentSong) >= 0) {
      // 找到新的高亮歌曲
      final int index = mDatas.indexOf(currentSong) + 1;
      final int lastIndex = mDatas.indexOf(mLastPlaySong) + 1;

      SongViewHolder newHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof SongViewHolder) {
        newHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
      }
      SongViewHolder oldHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof SongViewHolder) {
        oldHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
      }

      if (newHolder != null) {
        newHolder.mName.setTextColor(getHighLightTextColor());
        newHolder.mIndicator.setVisibility(View.VISIBLE);
      }

      if (oldHolder != null) {
        oldHolder.mName.setTextColor(getTextColorPrimary());
        oldHolder.mIndicator.setVisibility(View.GONE);
      }
      mLastPlaySong = currentSong;
    }
  }

  static class SongViewHolder extends BaseViewHolder {

    @BindView(R.id.song_title)
    TextView mName;
    @BindView(R.id.song_other)
    TextView mOther;
    @BindView(R.id.song_head_image)
    SimpleDraweeView mImage;
    @BindView(R.id.song_button)
    ImageButton mButton;
    @BindView(R.id.item_root)
    View mContainer;
    @BindView(R.id.indicator)
    View mIndicator;


    SongViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class HeaderHolder extends BaseViewHolder {

    View mRoot;
    @BindView(R.id.divider)
    View mDivider;
    @BindView(R.id.play_shuffle_button)
    ImageView mIvShuffle;
    @BindView(R.id.tv_shuffle_count)
    TextView mTvShuffle;

    HeaderHolder(View itemView) {
      super(itemView);
      mRoot = itemView;
    }
  }
}
