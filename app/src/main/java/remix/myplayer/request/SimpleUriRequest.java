package remix.myplayer.request;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.functions.Consumer;
import remix.myplayer.bean.netease.SearchRequest;
import remix.myplayer.request.network.RxUtil;

public abstract class SimpleUriRequest extends ImageUriRequest<Uri> {
    private SearchRequest mRequest;
    public SimpleUriRequest(@NonNull SearchRequest request) {
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
