package remix.myplayer.listener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import com.soundcloud.android.crop.Crop;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

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
        ArrayList<Integer> idList = new ArrayList<>();
        //根据不同参数获得歌曲id列表
        idList = MediaStoreUtil.getSongIdList(mId,mType);


        switch (item.getItemId()) {
            //播放
            case R.id.menu_play:
                if((idList == null || idList.size() == 0)){
                    ToastUtil.show(mContext,R.string.list_isempty);
                    return true;
                }
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", 0);
                intent.putExtras(arg);
                Global.setPlayQueue(idList,mContext,intent);
                break;
            //添加到播放队列
            case R.id.menu_add:
                if((idList == null || idList.size() == 0)){
                    ToastUtil.show(mContext,R.string.list_isempty);
                    return true;
                }
                ToastUtil.show(mContext,mContext.getString(R.string.add_song_playinglist_success,Global.AddSongToPlayQueue(idList)));
                break;
            //删除
            case R.id.menu_delete:
                if(mId == Global.mMyLoveID){
                    ToastUtil.show(mContext, mContext.getString(R.string.mylove_cant_delelte));
                    return true;
                }
                ToastUtil.show(mContext,(mType != Constants.PLAYLIST ? MediaStoreUtil.delete(mId , mType) : PlayListUtil.deletePlayList(mId)) ?
                        R.string.delete_success :
                        R.string.delete_error);
                break;
            //设置封面
            case R.id.menu_album_thumb:
                Global.mSetCoverID = mId;
                Global.mSetCoverType = mType;
                Global.mSetCoverName = mKey;
                Intent ori = ((Activity)mContext).getIntent();
                ori.putExtra("ID",mId);
                ((Activity)mContext).setIntent(ori);
                Crop.pickImage((Activity) mContext, Crop.REQUEST_PICK);
                break;
            default:
                ToastUtil.show(mContext,"click " + item.getTitle());
        }
        return true;
    }
}
