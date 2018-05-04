package remix.myplayer.request.network;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import remix.myplayer.bean.netease.SearchRequest;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.RequestConfig;

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest extends ImageUriRequest<Bitmap> {
    private SearchRequest mRequest;

    public RemoteUriRequest(@NonNull SearchRequest request, @NonNull RequestConfig config){
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
