package remix.myplayer.listener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-1-25.
 */
public class AlbumArtistFolderListener implements PopupMenu.OnMenuItemClickListener {
    private Context mContext;
    //专辑id 艺术家id 歌曲id 文件夹position
    private int mId;
    //0:专辑 1:歌手 2:文件夹 3:播放列表
    private int mType;
    private String mKey;
    public AlbumArtistFolderListener(Context Context, int id, int type, String key) {
        this.mContext = Context;
        this.mId = id;
        this.mType = type;
        this.mKey = key;
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        ArrayList<MP3Item> list = new ArrayList<>();
        ArrayList<Long> ids = new ArrayList<Long>();
        String name = null;
        //根据不同参数获得mp3信息列表
        //艺术家与专辑
        if(mType == Constants.ARTIST_HOLDER || mType == Constants.ALBUM_HOLDER) {
            list = DBUtil.getMP3InfoByArtistIdOrAlbumId(mId, mType);
            for(MP3Item info : list)
                ids.add(info.getId());
        }
        //文件夹
        else if(mType == Constants.FOLDER_HOLDER) {
            list = DBUtil.getMP3ListByIds(DBUtil.getIdsByFolderName(mKey,mId));
            for(MP3Item info : list)
                ids.add(info.getId());
        }
        //播放列表
        else {
            Iterator it = PlayListActivity.getPlayList().keySet().iterator();
            for(int i = 0 ; i <= mId ; i++) {
                it.hasNext();
                name = it.next().toString();
            }
            for(PlayListItem tmp : PlayListActivity.getPlayList().get(name))
                ids.add((long)tmp.getId());
        }

        if(ids == null || ids.size() == 0){
            Toast.makeText(mContext,mContext.getString(R.string.list_isempty),Toast.LENGTH_SHORT).show();
            return true;
        }
        switch (item.getItemId()) {
            //播放
            case R.id.menu_play:
                Global.setPlayingList((ArrayList) ids.clone());
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", 0);
                intent.putExtras(arg);
                mContext.sendBroadcast(intent);
                break;
            //添加到播放列表
            case R.id.menu_add:
                Global.mPlayingList.addAll(ids);
                Global.setPlayingList(Global.mPlayingList);
                break;
            //删除
            case R.id.menu_delete:
                if(mType != Constants.PLAYLIST_HOLDER) {
                    DBUtil.deleteSong(
                            mType == Constants.ALBUM_HOLDER || mType == Constants.ARTIST_HOLDER ? mId  + "" : mKey ,
                            mType);
                }
                else {
                    if(name != null && !name.equals("")) {
                        PlayListActivity.getPlayList().remove(name);
                        if(PlayListActivity.mInstance != null && PlayListActivity.mInstance.getAdapter() != null)
                            PlayListActivity.mInstance.UpdateAdapter();
                        XmlUtil.updatePlaylist();
                    }
                }
                break;
            //设置专辑封面
            case R.id.menu_album_thumb:
                Global.mAlbumArtistID = mId;
                Global.mAlbunOrArtist = mType;
                Global.mAlbumArtistName = mKey;
                Intent ori = ((Activity)mContext).getIntent();
                ori.putExtra("ID",mId);
                ((Activity)mContext).setIntent(ori);
                Crop.pickImage((Activity) mContext,Crop.REQUEST_PICK);

//                Intent getImageIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                getImageIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
//                /* 取得相片后返回本画面 */
//                if(mContext instanceof Activity)
//                    ((Activity)mContext).startActivityForResult(getImageIntent, Constants.SELECL_ALBUM_IMAGE);
                break;
            default:
                Toast.makeText(mContext, "Click " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
