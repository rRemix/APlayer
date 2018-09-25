package remix.myplayer.request;

import android.annotation.SuppressLint;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.PlayListUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {
    public PlayListUriRequest(SimpleDraweeView image, UriRequest request, RequestConfig config) {
        super(image,request,config);
    }

    @Override
    public void onError(String errMsg) {
//        mImage.setImageURI(Uri.EMPTY);
    }

    public Disposable loadImage(){
        return Observable.concat(
                getCustomThumbObservable(mRequest),
                Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mRequest.getID()),mRequest.getID()))
                        .concatMapDelayError(song -> getCoverObservable(getSearchRequestWithAlbumType(song))))
                .firstOrError()
                .toObservable()
                .compose(RxUtil.applyScheduler())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    protected void onStart() {

                    }

                    @Override
                    public void onNext(String s) {
                        onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        PlayListUriRequest.this.onError(e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @SuppressLint("CheckResult")
    @Override
    public void load() {
        Observable.concat(
                getCustomThumbObservable(mRequest),
                Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mRequest.getID()),mRequest.getID()))
                        .concatMapDelayError(song -> getCoverObservable(getSearchRequestWithAlbumType(song))))
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
