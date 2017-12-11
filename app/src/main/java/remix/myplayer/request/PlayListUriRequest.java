package remix.myplayer.request;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.Observable;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.PlayListUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {

    public PlayListUriRequest(SimpleDraweeView image, NSearchRequest request,RequestConfig config) {
        super(image,request,config);
    }

    @Override
    public void load() {
        Observable.concat(getCustomThumbObservable(mRequest),Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mRequest.getID())))
        .flatMap(song -> getThumbObservable(getSearchRequestWithAlbumType(song)))).firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));

    }
}
