package remix.myplayer.ui.adapter;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.net.Uri;
import android.view.View;
import java.util.ArrayList;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ItemSongChooseBinding;
import remix.myplayer.misc.interfaces.OnSongChooseListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.theme.TintHelper;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/21 10:02
 */

public class SongChooseAdapter extends BaseAdapter<Song, SongChooseAdapter.SongChooseHolder> {

  private OnSongChooseListener mCheckListener;
  private ArrayList<Integer> mCheckSongIdList = new ArrayList<>();

  public SongChooseAdapter(int layoutID, OnSongChooseListener l) {
    super(layoutID);
    mCheckListener = l;
  }

  public ArrayList<Integer> getCheckedSong() {
    return mCheckSongIdList;
  }

  @Override
  protected void convert(final SongChooseHolder holder, Song song, int position) {
    //歌曲名
    holder.binding.itemSong.setText(song.getShowName());
    //艺术家
    holder.binding.itemArtist.setText(song.getArtist());
    //封面
    holder.binding.itemImg.setImageURI(Uri.EMPTY);

    new LibraryUriRequest(holder.binding.itemImg,
        getSearchRequestWithAlbumType(song),
        new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
    //选中歌曲
    holder.binding.getRoot().setOnClickListener(v -> {
      holder.binding.checkbox.setChecked(!holder.binding.checkbox.isChecked());
      mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
    });

    final int audioId = song.getId();
    TintHelper.setTint(holder.binding.checkbox, ThemeStore.getAccentColor(), !ThemeStore.isLightTheme());
    holder.binding.checkbox.setOnCheckedChangeListener(null);
    holder.binding.checkbox.setChecked(mCheckSongIdList != null && mCheckSongIdList.contains(audioId));
    holder.binding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked && !mCheckSongIdList.contains(audioId)) {
        mCheckSongIdList.add(audioId);
      } else if (!isChecked) {
        mCheckSongIdList.remove(Integer.valueOf(audioId));
      }
      mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
    });
  }

  static class SongChooseHolder extends BaseViewHolder {

    private final ItemSongChooseBinding binding;

    SongChooseHolder(View itemView) {
      super(itemView);
      binding = ItemSongChooseBinding.bind(itemView);
    }
  }
}
