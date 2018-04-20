package remix.myplayer.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.soundcloud.android.crop.Crop;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Collections;

import remix.myplayer.R;
import remix.myplayer.bean.CustomThumb;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.AddtoPlayListDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;

/**
 * Created by Remix on 2018/3/5.
 */

public class SongPopupListener implements PopupMenu.OnMenuItemClickListener {
    private String mPlayListName;
    private boolean mIsDeletePlayList;
    private Song mSong;
    private Context mContext;

    public SongPopupListener(Context context, Song song,boolean isDeletePlayList,String playListName) {
        mIsDeletePlayList = isDeletePlayList;
        mPlayListName = playListName;
        mSong = song;
        mContext = context;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_next:
                MobclickAgent.onEvent(mContext,"Share");
                Intent intent = new Intent(MusicService.ACTION_CMD);
                intent.putExtra("Control", Constants.ADD_TO_NEXT_SONG);
                intent.putExtra("song",mSong);
                mContext.sendBroadcast(intent);
                break;
            case R.id.menu_add_to_playlist:
                MobclickAgent.onEvent(mContext,"AddtoPlayList");
                Intent intentAdd = new Intent(mContext,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putSerializable("list", new ArrayList<>(Collections.singletonList(mSong.getId())));
                intentAdd.putExtras(ardAdd);
                mContext.startActivity(intentAdd);
                break;
            case R.id.menu_add_to_play_queue:
                ToastUtil.show(mContext,mContext.getString(R.string.add_song_playqueue_success, Global.AddSongToPlayQueue(Collections.singletonList(mSong.getId()))));
                break;
            case R.id.menu_album_thumb:
                CustomThumb thumbBean = new CustomThumb(mSong.getAlbumId(),Constants.ALBUM,mSong.getAlbum());
                Intent thumbIntent = ((Activity)mContext).getIntent();
                thumbIntent.putExtra("thumb",thumbBean);
                ((Activity)mContext).setIntent(thumbIntent);
                Crop.pickImage((Activity) mContext, Crop.REQUEST_PICK);
                break;
            case R.id.menu_ring:
                MobclickAgent.onEvent(mContext,"Ring");
                MediaStoreUtil.setRing(mContext,mSong.getId());
                break;
            case R.id.menu_share:
                MobclickAgent.onEvent(mContext,"Share");
                mContext.startActivity(
                        Intent.createChooser(Util.createShareSongFileIntent(mSong, mContext), null));
                break;
            case R.id.menu_delete:
                MobclickAgent.onEvent(mContext,"Delete");
                try {
                    String title = mContext.getString(R.string.confirm_delete_from_playlist_or_library,mIsDeletePlayList ? mPlayListName : "曲库");
                    new MaterialDialog.Builder(mContext)
                            .content(title)
                            .buttonRippleColor(ThemeStore.getRippleColor())
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .checkBoxPromptRes(R.string.delete_source, false, null)
                            .onAny((dialog, which) -> {
                                if(which == POSITIVE){
                                    MobclickAgent.onEvent(mContext,"Delete");
                                    boolean deleteSuccess = !mIsDeletePlayList ?
                                            MediaStoreUtil.delete(mSong.getId() , Constants.SONG,dialog.isPromptCheckBoxChecked()) > 0 :
                                            PlayListUtil.deleteSong(mSong.getId(),mPlayListName);

                                    ToastUtil.show(mContext,deleteSuccess ? R.string.delete_success : R.string.delete_error);
                                }
                            })
                            .backgroundColorAttr(R.attr.background_color_3)
                            .positiveColorAttr(R.attr.text_color_primary)
                            .negativeColorAttr(R.attr.text_color_primary)
                            .contentColorAttr(R.attr.text_color_primary)
                            .theme(ThemeStore.getMDDialogTheme())
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
        return true;
    }
}
