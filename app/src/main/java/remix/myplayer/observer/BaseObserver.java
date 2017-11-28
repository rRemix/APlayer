package remix.myplayer.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import java.util.concurrent.TimeUnit;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Remix on 2017/11/20.
 */

public abstract class BaseObserver extends ContentObserver {
    Handler mHandler;
    Emitter<Uri> mEmitter;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public BaseObserver(Handler handler) {
        super(handler);
        mHandler = handler;
        Observable.create((ObservableOnSubscribe<Uri>) emitter -> mEmitter = emitter)
                .debounce(500, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
                .filter(this::onFilter)
                .subscribe(this::onAccept);
    }

    abstract void onAccept(Uri uri);
    abstract boolean onFilter(Uri uri);

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if(!selfChange){
            mEmitter.onNext(uri);
        }
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }
}
