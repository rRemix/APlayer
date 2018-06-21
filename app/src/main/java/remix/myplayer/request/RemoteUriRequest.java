package remix.myplayer.request;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import remix.myplayer.request.network.RxUtil;

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest extends ImageUriRequest<Bitmap> {
    private UriRequest mRequest;

    public RemoteUriRequest(@NonNull UriRequest request, @NonNull RequestConfig config){
        super(config);
        mRequest = request;
    }

    @Override
    public void load() {
        getThumbBitmapObservable(mRequest)
                .compose(RxUtil.applyScheduler())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }


}
