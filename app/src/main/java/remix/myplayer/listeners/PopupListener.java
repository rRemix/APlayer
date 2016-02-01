package remix.myplayer.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.PlayListItem;
import remix.myplayer.utils.Utility;

/**
 * Created by taeja on 16-1-25.
 */
public class PopupListener implements PopupMenu.OnMenuItemClickListener {
    private Context mContext;
    private int mId;
    //0:专辑 1:歌手 2:文件夹
    private int mType;
    private String mKey;
    public PopupListener(Context Context,int id,int type,String key) {
        this.mContext = Context;
        this.mId = id;
        this.mType = type;
        this.mKey = key;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        ArrayList<MP3Info> list = new ArrayList<>();
        ArrayList<Long> ids = new ArrayList<Long>();
        if(mType <= Utility.ARTIST_HOLDER) {
            list = Utility.getMP3InfoByArtistIdOrAlbumId(mId, mType);
            for(MP3Info info : list)
                ids.add(info.getId());
        }
        else if(mType == Utility.FOLDER_HOLDER)
        {
            list = Utility.getMP3ListByFolder(Utility.mFolderList.get(mId));
            for(MP3Info info : list)
                ids.add(info.getId());
        }
        else
        {
            String name = null;
            Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
            for(int i = 0 ; i <= mId ; i++) {
                it.hasNext();
                name = it.next().toString();
            }
            for(PlayListItem tmp : PlayListActivity.mPlaylist.get(name))
                ids.add((long)tmp.getId());
        }
        switch (item.getItemId()) {
            //播放
            case R.id.menu_play:

                Utility.mPlayList = (ArrayList) ids.clone();
                MusicService.mInstance.UpdateNextSong(0);
                Intent intent = new Intent(Utility.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Utility.PLAYSELECTEDSONG);
                arg.putInt("Position", 0);
                intent.putExtras(arg);
                mContext.sendBroadcast(intent);
                break;
            //添加到播放列表
            case R.id.menu_add:
                Utility.mPlayList.addAll(ids);
                break;
            //删除
            case R.id.menu_delete:
                Utility.deleteSong(mKey,mType);
                break;
            default:
                Toast.makeText(mContext, "Click " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
