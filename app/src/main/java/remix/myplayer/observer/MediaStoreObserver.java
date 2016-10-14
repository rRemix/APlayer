package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.XmlUtil;

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
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
    }

    @Override
    public void onChange(boolean selfChange) {
        LogUtil.d("ThreadId","id in observer: " + Thread.currentThread().getId());
        if(!selfChange){
            Global.mAllSongList = MediaStoreUtil.getAllSongsId();

            mHandler.sendEmptyMessage(Constants.UPDATE_FOLDER);
            //检查正在播放列表中是否有歌曲删除
            new Thread(){
                @Override
                public void run() {
                    try {
                        boolean needupdate = false;
                        if(Global.mPlayQueue != null){
//                            for(int i = Global.mPlayQueue.size() - 1; i >= 0 ; i--){
//                                MP3Item temp = MediaStoreUtil.getMP3InfoById(Global.mPlayQueue.get(i));
//                                if(temp == null) {
//                                    Global.mPlayQueue.remove(i);
//                                    needupdate = true;
//                                }
//                            }
                            if(needupdate){
                                XmlUtil.updatePlayQueue();
                                mHandler.sendEmptyMessage(Constants.UPDATE_PLAYINGLIST);
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();

            //检查播放列表中是否有歌曲删除
//            new Thread(){
//                @Override
//                public void run() {
//                    try {
//                        Iterator it = PlayListActivity.getPlayList().keySet().iterator();
//                        ArrayList<PlayListItem> list = new ArrayList<>();
//
//                        boolean needupdate = false;
//                        while (it.hasNext()){
//                            list = PlayListActivity.getPlayList().get(it.next());
//                            if(list != null){
//                                for(int i = list.size() - 1 ; i >= 0  ; i--){
//                                    MP3Item temp = MediaStoreUtil.getMP3InfoById(list.get(i).getId());
//                                    if(temp == null || temp.equals(new MP3Item())){
//                                        list.remove(i);
//                                        needupdate = true;
//                                    }
//                                }
//                            }
//                        }
//                        if(needupdate){
//                            XmlUtil.updatePlaylist();
//                            mHandler.sendEmptyMessage(Constants.UPDATE_PLAYLIST);
//                        }
//
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
//            }.start();
        }
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }
}
