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
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
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

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                    Global.AllSongList = MediaStoreUtil.getAllSongsIdWithFolder();
                    e.onNext(1);
                }
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<Integer>() {
                @Override
                public void accept(Integer integer) throws Exception {
                    mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                }
            });
        }
    };

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if(!selfChange && uri != null && uri.toString().contains("content://media/external")){
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable,600);
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
