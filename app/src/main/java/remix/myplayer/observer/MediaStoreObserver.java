package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;

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
        LogUtil.d("ThreadId","id in observer: " + Thread.currentThread().getId());
        if(!selfChange){
            new Thread(){
                @Override
                public void run() {
                    Global.mAllSongList = MediaStoreUtil.getAllSongsId();
                    mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                }
            }.start();
        }
    }

    @Override
    public void onChange(boolean selfChange) {
       super.onChange(selfChange);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }
}
