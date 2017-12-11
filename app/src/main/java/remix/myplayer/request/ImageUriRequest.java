package remix.myplayer.request;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.common.util.UriUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import okhttp3.ResponseBody;
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

public abstract class ImageUriRequest<T> {
    public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),125);
    public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),45);
    public static final int URL_PLAYLIST = 1000;
    public static final int URL_ALBUM = 10;
    public static final int URL_ARTIST = 100;

    public static String AUTO_DOWNLOAD_ALBUM = SPUtil.getValue(APlayerApplication.getContext(),"Setting", SPUtil.SPKEY.AUTO_DOWNLOAD_ALBUM_COVER,APlayerApplication.getContext().getString(R.string.wifi_only));

    protected RequestConfig mConfig = DEFAULT_CONFIG;

    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
            .forceDownload(true).build();

    public ImageUriRequest(RequestConfig config){
        mConfig = config;
    }

    public ImageUriRequest(){
    }

    public abstract void onError(String errMsg);

    public abstract void onSuccess(T url);

    public abstract void load();

    protected Observable<String> getThumbObservable(NSearchRequest request){
       return Observable.concat(getCustomThumbObservable(request),getContentThumbObservable(request),getNetworkThumbObservable(request))
               .firstOrError()
               .toObservable();
    }

    protected Observable<String> getCustomThumbObservable(NSearchRequest request){
        return new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                //是否设置过自定义封面
                File customImage = ImageUriUtil.getCustomThumbIfExist(request.getID(),request.getLType());
                if(customImage != null && customImage.exists()){
                    observer.onNext("file://" + customImage.getAbsolutePath());
                }
                observer.onComplete();
            }
        };
    }

    /**
     * 查询本地数据库
     * @param request
     * @return
     */
    private Observable<String> getContentThumbObservable(NSearchRequest request){
        return new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                String imageUrl = "";
                if(request.getLType() == URL_ALBUM){
                    //专辑封面
                    Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), request.getID());
                    if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                        imageUrl = uri.toString();
                    }
                } else {
                    //艺术家封面
                    File artistThumb = ImageUriUtil.getArtistThumbInMediaCache(request.getID());
                    if(artistThumb != null && artistThumb.exists()){
                        imageUrl = "file://" + artistThumb.getAbsolutePath();
                    }
                }
                if(!TextUtils.isEmpty(imageUrl)) {
                    observer.onNext(imageUrl);
                }
                observer.onComplete();
            }
        };
    }

    private Observable<String> getNetworkThumbObservable(NSearchRequest request){
        return Observable.concat(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                String imageUrl = SPUtil.getValue(APlayerApplication.getContext(),"HttpCache",request.getKey(),"");
                if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
                    observer.onNext(imageUrl);
                }
                observer.onComplete();
            }},Observable.just(isAutoDownloadCover())
                .filter(aBoolean -> aBoolean)
                .flatMap(aBoolean -> HttpClient.getNeteaseApiservice()
                        .getNeteaseSearch(request.getKey(), 0, 1, request.getNType())
                        .map(responseBody -> parseNetworkImageUrl(request, responseBody))
        .firstElement().toObservable()));

    }

    @Nullable
    private String parseNetworkImageUrl(NSearchRequest request, ResponseBody body) throws IOException {
        String imageUrl = "";
        if (request.getNType() == 1) {
            //搜索的是歌曲
            NSongSearchResponse response = new Gson().fromJson(body.string(), NSongSearchResponse.class);
            imageUrl =  response.result.songs.get(0).album.picUrl;
        } else if (request.getNType() == 10) {
            //搜索的是专辑
            NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
            imageUrl = response.result.albums.get(0).picUrl;
        } else if (request.getNType() == 100) {
            //搜索的是艺术家
            NArtistSearchResponse response = new Gson().fromJson(body.string(), NArtistSearchResponse.class);
            imageUrl = response.getResult().getArtists().get(0).getPicUrl();
        }
        if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
            SPUtil.putValue(APlayerApplication.getContext(),"HttpCache",request.getKey(),imageUrl);
        }
        return imageUrl;
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
