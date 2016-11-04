package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2016/10/19.
 */

public class DBObserver extends ContentObserver {
    private Handler mHandler;
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DBObserver(Handler handler) {
        super(handler);
        mHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {

    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if(!selfChange){
            new Thread(){
                @Override
                public void run() {
                    Global.mPlayList = PlayListUtil.getAllPlayListInfo();
                    Global.mPlayQueue = PlayListUtil.getIDList(Global.mPlayQueueID);
                    mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                }
            }.start();
        }

//        if(!selfChange){
//            switch (DBContentProvider.mUriMatcher.match(uri)){
//                //更新播放列表
//                case DBContentProvider.PLAY_LIST_MULTIPLE:
//                case DBContentProvider.PLAY_LIST_SINGLE:
//                    Global.mPlayList = PlayListUtil.getAllPlayListInfo();
//                    break;
//                //更新播放队列
//                case DBContentProvider.PLAY_LIST_SONG_MULTIPLE:
//                case DBContentProvider.PLAY_LIST_SONG_SINGLE:
//                    Global.mPlayQueue = PlayListUtil.getIDList(Global.mPlayQueueID);
//                    break;
//            }
//        }
    }

}
