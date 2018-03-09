package remix.myplayer.listener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.AddtoPlayListDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

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
            case R.id.menu_add:
                MobclickAgent.onEvent(mContext,"AddtoPlayList");
                Intent intentAdd = new Intent(mContext,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putInt("Id",mSong.getId());
                intentAdd.putExtras(ardAdd);
                mContext.startActivity(intentAdd);
                break;
            case R.id.menu_ring:
                MobclickAgent.onEvent(mContext,"Ring");
                MediaStoreUtil.setRing(mContext,mSong.getId());
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
