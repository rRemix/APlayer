package remix.myplayer.ui.adapter;

import static remix.myplayer.helper.MusicServiceRemote.setPlayQueue;
import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColor;
import static remix.myplayer.theme.ThemeStore.getTextColorPrimary;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import com.github.promeg.pinyinhelper.Pinyin;
import java.util.ArrayList;
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
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-6-24.
 */
@SuppressLint("RestrictedApi")
public class ChildHolderAdapter extends HeaderAdapter<Song, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  private int mType;
  private String mArg;

  private Song mLastPlaySong = MusicServiceRemote.getCurrentSong();

  public ChildHolderAdapter(Context context, int layoutId, int type, String arg,
      MultipleChoice multiChoice, RecyclerView recyclerView) {
    super(context, layoutId, multiChoice, recyclerView);
    this.mType = type;
    this.mArg = arg;
  }

  @Override
  public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return viewType == TYPE_HEADER ?
        new SongAdapter.HeaderHolder(
            LayoutInflater.from(mContext).inflate(R.layout.layout_header_1, parent, false)) :
        new ChildHolderViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_child_holder, parent, false));
  }

  @Override
  protected void convert(final BaseViewHolder baseHolder, final Song song, int position) {
    if (position == 0) {
      final SongAdapter.HeaderHolder headerHolder = (SongAdapter.HeaderHolder) baseHolder;
      //没有歌曲时隐藏
      if (mDatas == null || mDatas.size() == 0) {
        headerHolder.mRoot.setVisibility(View.GONE);
        return;
      }

      headerHolder.mShuffleIv.setImageDrawable(
          Theme.tintVectorDrawable(mContext, R.drawable.ic_shuffle_white_24dp,
              ThemeStore.getAccentColor())
      );

      //显示当前排序方式
      headerHolder.mRoot.setOnClickListener(v -> {
        //设置正在播放列表
        ArrayList<Integer> ids = new ArrayList<>();
        for (Song info : mDatas) {
          ids.add(info.getId());
        }
        if (ids.size() == 0) {
          ToastUtil.show(mContext, R.string.no_song);
          return;
        }
        setPlayQueue(ids, makeCmdIntent(Command.NEXT, true));
      });
      return;
    }

    final ChildHolderViewHolder holder = (ChildHolderViewHolder) baseHolder;
    if (song == null || song.getId() < 0 || song.Title.equals(mContext.getString(R.string.song_lose_effect))) {
      holder.mTitle.setText(R.string.song_lose_effect);
      holder.mButton.setVisibility(View.INVISIBLE);
    } else {
      holder.mButton.setVisibility(View.VISIBLE);

      //高亮
      final int primaryColor = getMaterialPrimaryColor();
      if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
        mLastPlaySong = song;
        holder.mTitle.setTextColor(primaryColor);
        holder.mIndicator.setVisibility(View.VISIBLE);
      } else {
        holder.mTitle.setTextColor(getTextColorPrimary());
        holder.mIndicator.setVisibility(View.GONE);
      }
      holder.mIndicator.setBackgroundColor(primaryColor);
      holder.mTitle.setText(song.getShowName());

      //设置标题
      holder.mTitle.setText(song.getShowName());

      if (holder.mButton != null) {
        //设置按钮着色
        int tintColor = ThemeStore.getLibraryBtnColor();
        Theme.tintDrawable(holder.mButton, R.drawable.icon_player_more, tintColor);

        holder.mButton.setOnClickListener(v -> {
          if (mChoice.isActive()) {
            return;
          }
          final PopupMenu popupMenu = new PopupMenu(mContext, holder.mButton, Gravity.END);
          popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
          popupMenu.setOnMenuItemClickListener(
              new SongPopupListener((AppCompatActivity) mContext, song, mType == Constants.PLAYLIST,
                  mArg));
          popupMenu.show();
        });
      }
    }

    if (holder.mContainer != null && mOnItemClickListener != null) {
      holder.mContainer.setOnClickListener(v -> {
        if (holder.getAdapterPosition() - 1 < 0) {
          ToastUtil.show(mContext, R.string.illegal_arg);
          return;
        }
        if (song != null && song.getId() > 0) {
          mOnItemClickListener.onItemClick(v, holder.getAdapterPosition() - 1);
        }
      });
      holder.mContainer.setOnLongClickListener(v -> {
        if (holder.getAdapterPosition() - 1 < 0) {
          ToastUtil.show(mContext, R.string.illegal_arg);
          return true;
        }
        mOnItemClickListener.onItemLongClick(v, holder.getAdapterPosition() - 1);
        return true;
      });
    }

    holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && mDatas.size() > 0 && position < mDatas.size()
        && mDatas.get(position - 1) != null) {
      String title = mDatas.get(position - 1).getTitle();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  public void updatePlayingSong() {
    final Song currentSong = MusicServiceRemote.getCurrentSong();
    if (currentSong.getId() == -1 || currentSong.getId() == mLastPlaySong.getId()) {
      return;
    }

    if (mDatas != null && mDatas.indexOf(currentSong) >= 0) {
      // 找到新的高亮歌曲
      final int index = mDatas.indexOf(currentSong) + 1;
      final int lastIndex = mDatas.indexOf(mLastPlaySong) + 1;

      ChildHolderViewHolder newHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof ChildHolderViewHolder) {
        newHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
      }
      ChildHolderViewHolder oldHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof ChildHolderViewHolder) {
        oldHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
      }

      if (newHolder != null) {
        newHolder.mTitle.setTextColor(getMaterialPrimaryColor());
        newHolder.mIndicator.setVisibility(View.VISIBLE);
      }

      if (oldHolder != null) {
        oldHolder.mTitle.setTextColor(getTextColorPrimary());
        oldHolder.mIndicator.setVisibility(View.GONE);
      }
      mLastPlaySong = currentSong;
    }
  }

  static class ChildHolderViewHolder extends BaseViewHolder {

    @BindView(R.id.album_holder_item_title)
    TextView mTitle;
    @BindView(R.id.song_item_button)
    public ImageButton mButton;
    public View mContainer;
    @BindView(R.id.indicator)
    View mIndicator;

    ChildHolderViewHolder(View itemView) {
      super(itemView);
      mContainer = itemView;
    }
  }
}