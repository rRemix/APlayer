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
import java.util.List;

import remix.myplayer.R;
import remix.myplayer.bean.CustomThumb;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.AddtoPlayListDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;

/**
 * Created by taeja on 16-1-25.
 */
public class AlbArtFolderPlaylistListener implements PopupMenu.OnMenuItemClickListener {
    private Context mContext;
    //专辑id 艺术家id 歌曲id 文件夹position
    private int mId;
    //0:专辑 1:歌手 2:文件夹 3:播放列表
    private int mType;
    //专辑名 艺术家名 文件夹position或者播放列表id
    private String mKey;
    public AlbArtFolderPlaylistListener(Context Context, int id, int type, String key) {
        this.mContext = Context;
        this.mId = id;
        this.mType = type;
        this.mKey = key;
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        List<Integer> idList = MediaStoreUtil.getSongIdList(mId,mType);
        //根据不同参数获得歌曲id列表
        switch (item.getItemId()) {
            //播放
            case R.id.menu_play:
                if((idList == null || idList.size() == 0)){
                    ToastUtil.show(mContext,R.string.list_is_empty);
                    return true;
                }
                Intent intent = new Intent(MusicService.ACTION_CMD);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", 0);
                intent.putExtras(arg);
                Global.setPlayQueue(idList,mContext,intent);
                break;
            //添加到播放队列
            case R.id.menu_add_to_play_queue:
                if((idList == null || idList.size() == 0)){
                    ToastUtil.show(mContext,R.string.list_is_empty);
                    return true;
                }
                ToastUtil.show(mContext,mContext.getString(R.string.add_song_playqueue_success,Global.AddSongToPlayQueue(idList)));
                break;
            //添加到播放列表
            case R.id.menu_add_to_playlist:
                Intent intentAdd = new Intent(mContext,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putSerializable("list",new ArrayList<>(idList));
                intentAdd.putExtras(ardAdd);
                mContext.startActivity(intentAdd);
                break;
            //删除
            case R.id.menu_delete:
                new MaterialDialog.Builder(mContext)
                        .content(mType == Constants.PLAYLIST ? R.string.confirm_delete_playlist : R.string.confirm_delete_from_library)
                        .buttonRippleColor(ThemeStore.getRippleColor())
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .checkBoxPromptRes(R.string.delete_source, false, null)
                        .onAny((dialog, which) -> {
                            if(which == POSITIVE){
                                MobclickAgent.onEvent(mContext,"Delete");
                                MobclickAgent.onEvent(mContext,"Delete");
                                if(mId == Global.MyLoveID && mType == Constants.PLAYLIST){
                                    ToastUtil.show(mContext, mContext.getString(R.string.mylove_cant_delete));
                                }
                                if(mType != Constants.PLAYLIST){
                                    ToastUtil.show(mContext,MediaStoreUtil.delete(mId , mType,dialog.isPromptCheckBoxChecked()) > 0 ? R.string.delete_success : R.string.delete_error);
                                } else {
                                    ToastUtil.show(mContext,PlayListUtil.deletePlayList(mId) ? R.string.delete_success : R.string.delete_error);
                                }
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            //设置封面
            case R.id.menu_album_thumb:
                CustomThumb thumbBean = new CustomThumb(mId,mType,mKey);
                Intent thumbIntent = ((Activity)mContext).getIntent();
                thumbIntent.putExtra("thumb",thumbBean);
                ((Activity)mContext).setIntent(thumbIntent);
//                try {
//                    Intent pickIntent = new Intent("android.intent.action.GET_CONTENT").setType("image/*");
//                    pickIntent.putExtra("test",1234);
//                    pickIntent.setExtrasClassLoader(CustomThumb.class.getClassLoader());
//                    pickIntent.putExtra("thumb",thumbBean);
//
//                    ((Activity)mContext).startActivityForResult(pickIntent, Crop.REQUEST_PICK);
//                }catch (Exception e){
//                    Toast.makeText(mContext, com.soundcloud.android.crop.R.string.crop__pick_error, Toast.LENGTH_SHORT).show();
//                }

                Crop.pickImage((Activity) mContext, Crop.REQUEST_PICK);
                break;
            default:
                ToastUtil.show(mContext,"click " + item.getTitle());
        }
        return true;
    }
}
