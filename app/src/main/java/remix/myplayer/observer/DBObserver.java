package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import remix.myplayer.db.DBContentProvider;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2016/10/19.
 */

public class DBObserver extends ContentObserver {
    private Handler mHandler;
    private int mMatch;
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DBObserver(Handler handler) {
        super(handler);
        mHandler = handler;
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Single.just(1)
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            switch (mMatch){
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
                            mHandler.sendEmptyMessage(Constants.UPDATE_PLAYLIST);
                        }
                    });
        }
    };

    @Override
    public void onChange(boolean selfChange, final Uri uri) {
        if(!selfChange){
            mMatch = DBContentProvider.mUriMatcher.match(uri);
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable,400);
        }
    }

}
