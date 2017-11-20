package remix.myplayer.observer;

import android.net.Uri;
import android.os.Handler;

import remix.myplayer.db.DBContentProvider;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2016/10/19.
 */

public class DBObserver extends BaseObserver {
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DBObserver(Handler handler) {
        super(handler);
    }

    @Override
    void onAccept(Uri uri) {
        int match = DBContentProvider.getUriMatcher().match(uri);
        switch (match){
            //更新播放列表
            case DBContentProvider.PLAY_LIST_MULTIPLE:
            case DBContentProvider.PLAY_LIST_SINGLE:
                Global.PlayList = PlayListUtil.getAllPlayListInfo();
                break;
            //更新播放队列
            case DBContentProvider.PLAY_LIST_SONG_MULTIPLE:
            case DBContentProvider.PLAY_LIST_SONG_SINGLE:
                break;
        }
        if(match != -1)
            mHandler.sendEmptyMessage(Constants.UPDATE_PLAYLIST);
    }

    @Override
    boolean onFilter(Uri uri) {
        return true;
    }
}
