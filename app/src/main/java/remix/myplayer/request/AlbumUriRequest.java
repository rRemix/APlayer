package remix.myplayer.request;

import android.content.ContentUris;
import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.lyric.network.HttpClient;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.model.netease.NAlbumSearchResponse;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public class AlbumUriRequest extends ImageUriRequest {
    private Album mAlbum;
    public AlbumUriRequest(SimpleDraweeView simpleDraweeView, Album album,RequestConfig config) {
        super(simpleDraweeView,config);
        mAlbum = album;
    }

    public AlbumUriRequest(SimpleDraweeView simpleDraweeView, Album album) {
        super(simpleDraweeView);
        mAlbum = album;
    }

    @Override
    public void load() {
        Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomThumbIfExist(mAlbum.getAlbumID(), Constants.URL_ALBUM);
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mAlbum.getAlbumID());
                if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                    observer.onNext(uri.toString());
                } else {
                    if(mConfig.isForceDownload()){
                        observer.onComplete();
                    } else{
                        observer.onError(new Throwable(""));
                    }
                }
            }
        }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mAlbum.getAlbum(),0,1,10)
                .doOnSubscribe(disposable -> {
                    //延迟 避免接口请求频繁
//                    Thread.sleep(24);
                })
                .map(body -> {
                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                    return response.result.albums.get(0).picUrl;
                }))
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));

    }

    public static Observable<String> getAlbumThumbObservable(Album album){
        return Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomThumbIfExist(album.getAlbumID(), Constants.URL_ALBUM);
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
//                String thumbUrl = getAlbumUrlByAlbumId(mAlbum.getAlbumID());
//                if(!TextUtils.isEmpty(thumbUrl)) {
//                    if(new File(thumbUrl).exists()) {
//                        observer.onNext("file://" + thumbUrl);
//                    }
//                }

                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), album.getAlbumID());
                if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                    observer.onNext(uri.toString());
                }
                observer.onComplete();
            }
        }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(album.getAlbum(),0,1,10)
                .doOnSubscribe(disposable -> {
                    //延迟 避免接口请求频繁
//                    Thread.sleep(24);
                })
                .map(body -> {
                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                    return response.result.albums.get(0).picUrl;
                }));
    }
}
