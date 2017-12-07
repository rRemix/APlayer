package remix.myplayer.request;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.model.netease.NAlbumSearchResponse;
import remix.myplayer.model.netease.NArtistSearchResponse;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.model.netease.NSongSearchResponse;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.util.Util.isWifi;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest {
    public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),125);
    public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),45);
    public static final int URL_PLAYLIST = 1000;
    public static final int URL_ALBUM = 10;
    public static final int URL_ARTIST = 100;

    public static String AUTO_DOWNLOAD_ALBUM = SPUtil.getValue(APlayerApplication.getContext(),"Setting", SPUtil.SPKEY.AUTO_DOWNLOAD_ALBUM_COVER,APlayerApplication.getContext().getString(R.string.wifi_only));

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
            //是否设置过自定义封面
            File customImage = ImageUriUtil.getCustomThumbIfExist(request.getID(),request.getLType());
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            //查询本地数据库
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                if(request.getLType() == URL_ALBUM){
                    //专辑封面
                    Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), request.getID());
                    if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                        observer.onNext(uri.toString());
                    } else {
                        observer.onComplete();
                    }
                } else {
                    //艺术家封面
                    File artistThumb = ImageUriUtil.getArtistThumbInMediaCache(request.getID());
                    if(artistThumb != null && artistThumb.exists()){
                        observer.onNext("file://" + artistThumb.getAbsolutePath());
                    } if(isAutoDownloadCover()){
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
                }))
                .map(s -> {
                    //对于网络请求 需要根据用户设置判断是否加载
                    if(UriUtil.isNetworkUri(Uri.parse(s)) && !isAutoDownloadCover()){
                        ImageRequest imageRequest = ImageRequest.fromUri(s);
                        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(imageRequest,null);
                        if(ImagePipelineFactory.getInstance().getMainBufferedDiskCache().containsSync(cacheKey) ||
                                ImagePipelineFactory.getInstance().getImagePipeline().isInDiskCacheSync(imageRequest.getSourceUri())){
                            return s;
                        } else {
                            return "";
                        }
                    } else {
                        return s;
                    }
                });
    }


    /**
     * 是否下载封面
     * @return
     */
    protected boolean isAutoDownloadCover() {
        Context context = APlayerApplication.getContext();
        return context.getString(R.string.always).equals(AUTO_DOWNLOAD_ALBUM) || (context.getString(R.string.wifi_only).equals(AUTO_DOWNLOAD_ALBUM) && isWifi(context));
    }

}
