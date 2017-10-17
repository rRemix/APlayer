package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
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
    private static OnChangeListener mPlayListListener;
    private static OnChangeListener mPlayListSongListener;
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
            Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                    switch (mMatch){
                        //更新播放列表
                        case DBContentProvider.PLAY_LIST_MULTIPLE:
                        case DBContentProvider.PLAY_LIST_SINGLE:
                            Global.PlayList = PlayListUtil.getAllPlayListInfo();
                            if(mPlayListListener != null)
                                mPlayListListener.OnChange();
                            break;
                        //更新播放队列
                        case DBContentProvider.PLAY_LIST_SONG_MULTIPLE:
                        case DBContentProvider.PLAY_LIST_SONG_SINGLE:
//                            Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                            if(mPlayListSongListener != null)
                                mPlayListSongListener.OnChange();
                            break;
                    }
                    e.onNext(1);
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            mHandler.sendEmptyMessage(Constants.UPDATE_CHILDHOLDER_ADAPTER);
                        }
                    });
        }
    };

    @Override
    public void onChange(boolean selfChange, final Uri uri) {
        if(!selfChange){
            mMatch = DBContentProvider.mUriMatcher.match(uri);
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable,500);

        }
    }

    public static void setPlayListListener(OnChangeListener playListListener) {
        mPlayListListener = playListListener;
    }

    public static void setPlayListSongListener(OnChangeListener playListSongListener) {
        mPlayListSongListener = playListSongListener;
    }

    public interface OnChangeListener{
        void OnChange();
    }

}
