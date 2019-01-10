package remix.myplayer.request;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.NonNull;
import io.reactivex.disposables.Disposable;
import remix.myplayer.request.network.RxUtil;

public abstract class SimpleUriRequest extends ImageUriRequest<Uri> {

  private UriRequest mRequest;

  public SimpleUriRequest(@NonNull UriRequest request) {
    super();
    mRequest = request;
  }

  @SuppressLint("CheckResult")
  @Override
  public Disposable load() {
    return getCoverObservable(mRequest)
        .compose(RxUtil.applyScheduler())
        .subscribe(s -> onSuccess(Uri.parse(s)),
            throwable -> onError(throwable.toString()));
  }
}
