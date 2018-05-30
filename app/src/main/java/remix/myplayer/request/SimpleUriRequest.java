package remix.myplayer.request;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.NonNull;

import remix.myplayer.request.network.RxUtil;

public abstract class SimpleUriRequest extends ImageUriRequest<Uri> {
    private NewUriRequest mRequest;
    public SimpleUriRequest(@NonNull NewUriRequest request) {
        super();
        mRequest = request;
    }

    @SuppressLint("CheckResult")
    @Override
    public void load() {
        getCoverObservable(mRequest)
                .compose(RxUtil.applyScheduler())
                .subscribe(s -> onSuccess(Uri.parse(s)),
                        throwable -> onError(throwable.toString()));
    }
}
