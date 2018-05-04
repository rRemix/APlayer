package remix.myplayer.request;

import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.Observable;
import remix.myplayer.bean.netease.SearchRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.PlayListUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {
    public PlayListUriRequest(SimpleDraweeView image, SearchRequest request, RequestConfig config) {
        super(image,request,config);
    }

    @Override
    public void onError(String errMsg) {
        mImage.setImageURI(Uri.EMPTY);
        LogUtil.d("Cover","Err: " + errMsg);
    }

    @Override
    public void load() {
        LogUtil.d("Cover","Request: " + mRequest);
        Observable.concat(
                getCustomThumbObservable(mRequest),
                Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mRequest.getID()),mRequest.getID()))
                        .concatMapDelayError(song -> getThumbObservable(getSearchRequestWithAlbumType(song))))
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
