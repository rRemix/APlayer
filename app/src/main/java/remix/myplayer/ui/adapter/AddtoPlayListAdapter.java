package remix.myplayer.ui.adapter;

import android.view.View;
import remix.myplayer.databinding.ItemPlaylistAddtoBinding;
import remix.myplayer.db.room.model.PlayList;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的适配器
 */
public class AddtoPlayListAdapter extends BaseAdapter<PlayList, AddtoPlayListAdapter.PlayListAddToHolder> {

  public AddtoPlayListAdapter(int layoutId) {
    super(layoutId);
  }


  @Override
  protected void convert(PlayListAddToHolder holder, PlayList playList, int position) {
    holder.binding.playlistAddtoText.setText(playList.getName());
    holder.binding.playlistAddtoText.setTag(playList.getId());
    holder.binding.itemRoot.setOnClickListener(v -> {
      if (mOnItemClickListener != null) {
        mOnItemClickListener.onItemClick(v, position);
      }
    });
  }


  static class PlayListAddToHolder extends BaseViewHolder {

    private final ItemPlaylistAddtoBinding binding;

    PlayListAddToHolder(View itemView) {
      super(itemView);
      binding = ItemPlaylistAddtoBinding.bind(itemView);
    }
  }
}
