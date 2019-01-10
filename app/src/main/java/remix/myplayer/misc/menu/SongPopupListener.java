package remix.myplayer.misc.menu;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;
import static remix.myplayer.service.MusicService.EXTRA_SONG;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import com.soundcloud.android.crop.Crop;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import java.util.Collections;
import java.util.concurrent.Callable;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.misc.CustomThumb;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.DeleteHelper;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.Tag;
import remix.myplayer.ui.dialog.AddtoPlayListDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2018/3/5.
 */

public class SongPopupListener
    implements PopupMenu.OnMenuItemClickListener {

  private String mPlayListName;
  private boolean mIsDeletePlayList;
  private Song mSong;
  private AppCompatActivity mActivity;
  private Tag mTag;

  public SongPopupListener(AppCompatActivity activity, Song song, boolean isDeletePlayList,
      String playListName) {
    mIsDeletePlayList = isDeletePlayList;
    mPlayListName = playListName;
    mSong = song;
    mActivity = activity;
    mTag = new Tag(activity, mSong);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_next:
        Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
            .putExtra(EXTRA_SONG, mSong));
        break;
      case R.id.menu_add_to_playlist:
        AddtoPlayListDialog.newInstance(Collections.singletonList(mSong.getId()))
            .show(mActivity.getSupportFragmentManager(), AddtoPlayListDialog.class.getSimpleName());
        break;
      case R.id.menu_add_to_play_queue:
        ToastUtil.show(mActivity, mActivity.getString(R.string.add_song_playqueue_success,
            PlayListUtil.addMultiSongs(Collections.singletonList(mSong.getId()),Constants.PLAY_QUEUE)));
        break;
      case R.id.menu_detail:
        mTag.detail();
        break;
      case R.id.menu_edit:
        mTag.edit();
        break;
      case R.id.menu_album_thumb:
        CustomThumb thumbBean = new CustomThumb(mSong.getAlbumId(), Constants.ALBUM,
            mSong.getAlbum());
        Intent thumbIntent = mActivity.getIntent();
        thumbIntent.putExtra("thumb", thumbBean);
        mActivity.setIntent(thumbIntent);
        Crop.pickImage(mActivity, Crop.REQUEST_PICK);
        break;
      case R.id.menu_ring:
        MediaStoreUtil.setRing(mActivity, mSong.getId());
        break;
      case R.id.menu_share:
        mActivity.startActivity(
            Intent.createChooser(Util.createShareSongFileIntent(mSong, mActivity), null));
        break;
      case R.id.menu_delete:
        String title = mActivity.getString(R.string.confirm_delete_from_playlist_or_library,
            mIsDeletePlayList ? mPlayListName : "曲库");
        Theme.getBaseDialog(mActivity)
            .content(title)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, SPUtil
                .getValue(App.getContext(), SETTING_KEY.NAME,
                    SETTING_KEY.DELETE_SOURCE, false), null)
            .onAny((dialog, which) -> {
              if (which == POSITIVE) {
                DeleteHelper.deleteSong(mSong.getId(),dialog.isPromptCheckBoxChecked(),mIsDeletePlayList,mPlayListName)
                    .subscribe(new Consumer<Boolean>() {
                      @Override
                      public void accept(Boolean success) throws Exception {
                        ToastUtil.show(mActivity,success ? R.string.delete_source: R.string.delete_error);
                      }
                    }, new Consumer<Throwable>() {
                      @Override
                      public void accept(Throwable throwable) throws Exception {
                        ToastUtil.show(mActivity,R.string.delete_error);
                      }
                    });
              }
            })
            .show();
        break;
    }
    return true;
  }
}
