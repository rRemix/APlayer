package remix.myplayer.observers;

import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.ui.dialog.PlayingListDialog;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-3-30.
 */
public class MediaStoreObserver extends ContentObserver {
    private Handler mHandler;
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaStoreObserver(Handler handler) {
        super(handler);
        mHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        Log.d("ThreadId","id in observer: " + Thread.currentThread().getId());
        if(!selfChange){
            DBUtil.mAllSongList = DBUtil.getAllSongsId();

            mHandler.sendEmptyMessage(Constants.UPDATE_FOLDER);

            //检查正在播放列表中是否有歌曲删除
            new Thread(){
                @Override
                public void run() {
                    try {
                        boolean needupdate = false;
                        if(DBUtil.mPlayingList != null){
                            for(int i = DBUtil.mPlayingList.size() - 1; i >= 0 ;i--){
                                MP3Info temp = DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(i));
                                if(temp == null) {
                                    DBUtil.mPlayingList.remove(i);
                                    needupdate = true;
                                }
                            }
                            if(needupdate){
                                XmlUtil.updatePlayingList();
                                mHandler.sendEmptyMessage(Constants.UPDATE_PLAYINGLIST);
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();

            //检查播放列表中是否有歌曲删除
            new Thread(){
                @Override
                public void run() {
                    try {
                        Iterator it = PlayListActivity.getPlayList().keySet().iterator();
                        ArrayList<PlayListItem> list = new ArrayList<>();

                        boolean needupdate = false;
                        while (it.hasNext()){
                            list = PlayListActivity.getPlayList().get(it.next());
                            if(list != null){
                                for(int i = list.size() - 1 ; i >= 0  ; i--){
                                    MP3Info temp = DBUtil.getMP3InfoById(list.get(i).getId());
                                    if(temp == null || temp.equals(new MP3Info())){
                                        list.remove(i);
                                        needupdate = true;
                                    }
                                }
                            }
                        }
                        if(needupdate){
                            XmlUtil.updatePlaylist();
                           mHandler.sendEmptyMessage(Constants.UPDATE_PLAYLIST);
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();


        }
    }

}
