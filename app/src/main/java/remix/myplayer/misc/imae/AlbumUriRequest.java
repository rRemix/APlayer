package remix.myplayer.misc.imae;

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
    public AlbumUriRequest(SimpleDraweeView simpleDraweeView, Album album) {
        super(simpleDraweeView);
        mAlbum = album;
    }

    @Override
    public void load() {
        getAlbumThumbObservable(mAlbum)
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
        if(true)
            return;

        //是否有自定义封面
//        long start = System.currentTimeMillis();
//        Handler handler = new Handler(Looper.getMainLooper());
//        new Thread(){
//            @Override
//            public void run() {
//                File customImage = ImageUriUtil.getCustomCoverIfExist(mAlbum.getAlbumID(), Constants.URL_ALBUM);
//                if(customImage != null && customImage.exists()){
//                    handler.post(() -> onSuccess("file://" + customImage.getAbsolutePath()));
//                    return;
//                }
//                LogUtil.e("ImageUriRequest","查找自定义封面耗时:" + (System.currentTimeMillis() - start));
//
//                long start1 = System.currentTimeMillis();
//                //是否存在于数据库
//                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mAlbum.getAlbumID());
//                boolean existInMediaStore = ImageUriUtil.isAlbumThumbExistInMediaCache(uri);
//                LogUtil.e("ImageUriRequest","查找内嵌封面耗时:" + (System.currentTimeMillis() - start1));
//                if(existInMediaStore){
//                    handler.post(() -> onSuccess(uri.toString()));
//                } else {
//                    //获得网易的封面
//                    HttpClient.getNeteaseApiservice()
//                            .getNeteaseSearch(mAlbum.getAlbum(),0,1,10)
//                            .compose(RxUtil.applyScheduler())
//                            .subscribe(new Consumer<ResponseBody>() {
//                                @Override
//                                public void accept(ResponseBody body) throws Exception {
//                                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
//                                    if(response != null && response.result.albums != null && response.result.albums.size() > 0)
//                                        handler.post(() -> onSuccess(response.result.albums.get(0).picUrl));
//                                }
//                            }, new Consumer<Throwable>() {
//                                @Override
//                                public void accept(Throwable throwable) throws Exception {
//                                    ToastUtil.show(APlayerApplication.getContext(),"onError:" + throwable);
//                                }
//                            });
//                }
//            }
//        }.start();
    }

    static Observable<String> getAlbumThumbObservable(Album album){
        return Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomCoverIfExist(album.getAlbumID(), Constants.URL_ALBUM);
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
                    Thread.sleep(24);
                })
                .map(body -> {
                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                    return response.result.albums.get(0).picUrl;
                }));
    }
}
