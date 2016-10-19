package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import remix.myplayer.db.DBContentProvider;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2016/10/19.
 */

public class DBObserver extends ContentObserver {

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DBObserver(Handler handler) {
        super(handler);

    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if(!selfChange){
            int match = DBContentProvider.mUriMatcher.match(uri);
            switch (DBContentProvider.mUriMatcher.match(uri)){
                //更新播放列表
                case DBContentProvider.PLAY_LIST_MULTIPLE:
                case DBContentProvider.PLAY_LIST_SINGLE:
                    Global.mPlayList = PlayListUtil.getAllPlayListInfo();
                    break;
                //更新播放队列
                case DBContentProvider.PLAY_LIST_SONG_MULTIPLE:
                case DBContentProvider.PLAY_LIST_SONG_SINGLE:
                    Global.mPlayQueue = PlayListUtil.getIDList(Global.mPlayQueueId);
                    break;
            }

        }
    }

}
