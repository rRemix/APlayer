package remix.myplayer.util;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Remix on 2017/11/20.
 */

public class RxUtil {

  private static final String TAG = "APlayerNetwork";

  private RxUtil() {
  }

  public static <T> ObservableTransformer<T, T> applyScheduler() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static <T> ObservableTransformer<T, T> applySchedulerToIO() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io());
  }

  public static <T> SingleTransformer<T, T> applySingleScheduler() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static <T> SingleTransformer<T, T> applySingleSchedulerToIO() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io());
  }

  private static <T> ObservableSource<T> createData(final T t) {
    return Observable.create(e -> {
      e.onNext(t);
      e.onComplete();
    });
  }
}
