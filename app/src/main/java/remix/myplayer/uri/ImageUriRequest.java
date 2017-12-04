package remix.myplayer.uri;

import android.content.ContentUris;
import android.net.Uri;

import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.APlayerApplication;
import remix.myplayer.lyric.network.HttpClient;
import remix.myplayer.model.netease.NAlbumSearchResponse;
import remix.myplayer.model.netease.NArtistSearchResponse;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.model.netease.NSongSearchResponse;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest {
    public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),125);
    public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),45);

    RequestConfig mConfig = DEFAULT_CONFIG;

    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
            .forceDownload(true).build();

    public ImageUriRequest(RequestConfig config){
        mConfig = config;
    }

    public ImageUriRequest(){
    }

    public abstract void onError(String errMsg);

    public abstract void onSuccess(String url);

    public abstract void load();

    protected Observable<String> getThumbObservable(NSearchRequest request){
        return Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomThumbIfExist(request.getID(),request.getLType());
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                if(request.getLType() == Constants.URL_ALBUM){
                    Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), request.getID());
                    if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                        observer.onNext(uri.toString());
                    } else {
                        if(mConfig != null && mConfig.isForceDownload()){
                            observer.onComplete();
                        } else{
                            observer.onError(new Throwable(""));
                        }
                    }
                } else {
                    File artistThumb = ImageUriUtil.getArtistThumbInMediaCache(request.getID());
                    if(artistThumb != null && artistThumb.exists()){
                        observer.onNext("file://" + artistThumb.getAbsolutePath());
                    } if(mConfig.isForceDownload()){
                        observer.onComplete();
                    } else{
                        observer.onError(new Throwable(""));
                    }
                }
            }
        }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(request.getKey(),0,1,request.getNType())
                .doOnSubscribe(disposable -> {
                    //延迟 避免接口请求频繁
//                    Thread.sleep(24);
                })
                .map(body -> {
                    if(request.getNType() == 1){
                        //搜索的是歌曲
                        NSongSearchResponse response = new Gson().fromJson(body.string(),NSongSearchResponse.class);
                        return response.result.songs.get(0).album.picUrl;
                    } else if (request.getNType() == 10){
                        //搜索的是专辑
                        NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                        return response.result.albums.get(0).picUrl;
                    } else if (request.getNType() == 100){
                        //搜索的是艺术家
                        NArtistSearchResponse response = new Gson().fromJson(body.string(),NArtistSearchResponse.class);
                        return response.getResult().getArtists().get(0).getPicUrl();
                    } else
                        return "";
                }));
    }

}
