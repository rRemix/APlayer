package remix.myplayer.misc.observer;

import android.annotation.SuppressLint;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import remix.myplayer.service.MusicService;

/**
 * Created by Remix on 2017/11/20.
 */

public abstract class BaseObserver extends ContentObserver {

  private Emitter<Uri> mEmitter;
  protected WeakReference<MusicService> mService;

  /**
   * Creates a content observer.
   *
   * @param handler The handler to run {@link #onChange} on, or null if none.
   */
  @SuppressLint("CheckResult")
  public BaseObserver(MusicService service, Handler handler) {
    super(handler);
    mService = new WeakReference<>(service);
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
    if (!selfChange) {
      mEmitter.onNext(uri);
    }
  }

  @Override
  public boolean deliverSelfNotifications() {
    return true;
  }
}
