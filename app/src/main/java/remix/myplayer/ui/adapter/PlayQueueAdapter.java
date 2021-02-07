package remix.myplayer.ui.adapter;

import android.view.View;
import java.util.Collections;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ItemPlayqueueBinding;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.Command;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayQueueAdapter extends BaseAdapter<Song, PlayQueueAdapter.PlayQueueHolder> {

  private int mAccentColor;
  private int mTextColor;

  public PlayQueueAdapter(int layoutId) {
    super(layoutId);
    mAccentColor = ThemeStore.getAccentColor();
    mTextColor = ThemeStore.getTextColorPrimary();
  }

  @Override
  protected void convert(final PlayQueueHolder holder, Song song, int position) {
    if (song == null) {
      //歌曲已经失效
      holder.binding.playlistItemName.setText(R.string.song_lose_effect);
      holder.binding.playlistItemArtist.setVisibility(View.GONE);
      return;
    }
    //设置歌曲与艺术家
    holder.binding.playlistItemName.setText(song.getShowName());
    holder.binding.playlistItemArtist.setText(song.getArtist());
    holder.binding.playlistItemArtist.setVisibility(View.VISIBLE);
    //高亮
    if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
      holder.binding.playlistItemName.setTextColor(mAccentColor);
    } else {
//                holder.mSong.setTextColor(Color.parseColor(ThemeStore.isDay() ? "#323335" : "#ffffff"));
      holder.binding.playlistItemName.setTextColor(mTextColor);
    }
    //删除按钮
    holder.binding.playqueueDelete.setOnClickListener(v -> {
      DatabaseRepository.getInstance()
          .deleteFromPlayQueue(Collections.singletonList(song.getId()))
          .compose(RxUtil.applySingleScheduler())
          .subscribe(num -> {
            //删除的是当前播放的歌曲
            if (num > 0 && MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
              Util.sendCMDLocalBroadcast(Command.NEXT);
            }
          });
    });
    if (mOnItemClickListener != null) {
      holder.binding.itemRoot.setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, holder.getAdapterPosition()));
    }

  }

  static class PlayQueueHolder extends BaseViewHolder {

    private final ItemPlayqueueBinding binding;

    public PlayQueueHolder(View view) {
      super(view);
      binding = ItemPlayqueueBinding.bind(view);
    }
  }
}
