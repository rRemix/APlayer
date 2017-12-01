package remix.myplayer.request;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import remix.myplayer.lyric.network.HttpClient;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.mp3.Artist;
import remix.myplayer.model.netease.NArtistSearchResponse;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public class ArtistUriRequest extends ImageUriRequest {
    private Artist mArtist;
    public ArtistUriRequest(SimpleDraweeView image, Artist artist) {
        super(image);
        mArtist = artist;
    }


    @Override
    public void load() {
        Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomThumbIfExist(mArtist.getArtistID(), Constants.URL_ARTIST);
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();

        }).switchIfEmpty(observer -> {
            File artistThumb = ImageUriUtil.getArtistThumbInMediaCache(mArtist.getArtistID());
            if(artistThumb != null && artistThumb.exists()){
                observer.onNext("file://" + artistThumb.getAbsolutePath());
            }
            observer.onComplete();
        }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mArtist.getArtist(),0,1,100)
                .doOnSubscribe(disposable -> {
                    //延迟 避免接口请求频繁
//                    Thread.sleep(24);
                })
                .map(body -> {
                    NArtistSearchResponse response = new Gson().fromJson(body.string(),NArtistSearchResponse.class);
                    return response.getResult().getArtists().get(0).getPicUrl();
                }))
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
