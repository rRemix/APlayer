package remix.myplayer.request;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.lastfm.LastFmAlbum;
import remix.myplayer.bean.lastfm.LastFmArtist;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NAlbumSearchResponse;
import remix.myplayer.bean.netease.NArtistSearchResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.App.IS_GOOGLEPLAY;
import static remix.myplayer.request.NewUriRequest.TYPE_NETEASE_ALBUM;
import static remix.myplayer.request.NewUriRequest.TYPE_NETEASE_ARTIST;
import static remix.myplayer.request.NewUriRequest.TYPE_NETEASE_SONG;
import static remix.myplayer.service.MusicService.copy;
import static remix.myplayer.util.Util.isWifi;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest<T> {
    public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(),125);
    public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(),45);
    public static final int URL_PLAYLIST = 1000;
    public static final int URL_ALBUM = 10;
    public static final int URL_ARTIST = 100;

    //自动下载封面
    public static String AUTO_DOWNLOAD_ALBUM = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,
            SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER, App.getContext().getString(R.string.always));
    //忽略内嵌
    public static boolean IGNORE_MEDIA_STORE = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,
            SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE,false);

    protected RequestConfig mConfig = DEFAULT_CONFIG;

    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
            .forceDownload(true).build();

    public ImageUriRequest(RequestConfig config){
        mConfig = config;
    }

    public ImageUriRequest(){
    }

    public abstract void onError(String errMsg);

    public abstract void onSuccess(T result);

    public abstract void load();

    protected Observable<String> getCoverObservable(NewUriRequest request){
       return Observable.concat(getCustomThumbObservable(request),getContentThumbObservable(request),getNetworkThumbObservable(request))
               .firstOrError()
               .toObservable();
    }

    Observable<String> getCustomThumbObservable(NewUriRequest request){
        return new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                //是否设置过自定义封面
                if(request.getSearchType() != URL_ALBUM){
                    File customImage = ImageUriUtil.getCustomThumbIfExist(request.getID(),request.getSearchType());
                    if(customImage != null && customImage.exists()){
                        observer.onNext("file://" + customImage.getAbsolutePath());
                    }
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
    private Observable<String> getContentThumbObservable(NewUriRequest request){
        return Observable.create(observer -> {
            String imageUrl = "";
            if(request.getSearchType() == URL_ALBUM){//专辑封面
                //忽略内嵌封面
                if(IGNORE_MEDIA_STORE){
                    Song song = MediaStoreUtil.getMP3InfoByAlbumId(request.getID());
                    imageUrl = resolveEmbeddedPicture(song);
                } else {
                    Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), request.getID());
                    if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                        imageUrl = uri.toString();
                    }
                }

            } else {//艺术家封面
                File artistThumb = ImageUriUtil.getArtistThumbInMediaCache(request.getID());
                if(artistThumb != null && artistThumb.exists()){
                    imageUrl = "file://" + artistThumb.getAbsolutePath();
                }
            }
            if(!TextUtils.isEmpty(imageUrl)) {
                observer.onNext(imageUrl);
            }
            observer.onComplete();
        });
    }

    /**
     * 内嵌封面
     * @param song
     * @return
     */
    private String resolveEmbeddedPicture(Song song){
        String imageUrl = null;
        try {
            if(song == null)
                return "";
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(song.getUrl());

            byte[] picture = retriever.getEmbeddedPicture();
            retriever.release();
            if(picture != null){
                Bitmap bitmap = BitmapFactory.decodeByteArray(picture,0,picture.length);
                //保存bitmap
                File cacheDir = DiskCache.getDiskCacheDir(App.getContext(), "embedded/");
                if(bitmap != null && (cacheDir.exists() || cacheDir.mkdirs())){
                    File original = new File(cacheDir,(song.getArtist() + " - " + song.getTitle()).replaceAll("/"," "));
                    if(original.exists()){
                        imageUrl = "file://" + original.getAbsolutePath();
                    } else {
                        FileOutputStream fileOutputStream = new FileOutputStream(original);
                        bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        imageUrl = "file://" + original.getAbsolutePath();
                    }
                }
            } else {
                File cover = fallback(song);
                if(cover != null && cover.exists())
                    imageUrl = "file://" + cover.getAbsolutePath();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return imageUrl;
    }

    private Observable<String> getNetworkThumbObservable(NewUriRequest request){
        return IS_GOOGLEPLAY ? getLastFMNetworkThumbObservable(request) : getNeteaseNetworkThumbObservable(request);
    }

    //lastFM
    private Observable<String> getLastFMNetworkThumbObservable(NewUriRequest request){
        return Observable.concat(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                String imageUrl = SPUtil.getValue(App.getContext(),SPUtil.COVER_KEY.COVER_NAME,request.getLastFMKey(),"");
                if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
                    observer.onNext(imageUrl);
                }
                observer.onComplete();
            }},Observable.just(isAutoDownloadCover() && !TextUtils.isEmpty(request.getLastFMKey()))
                .filter(aBoolean -> aBoolean)
                .flatMap(new Function<Boolean, ObservableSource<String>>() {
                    private Observable<ResponseBody> getObservable(NewUriRequest request){
                        return request.getSearchType() == ImageUriRequest.URL_ALBUM ?
                                HttpClient.getLastFMApiservice().getAlbumInfo(request.getAlbumName(),request.getArtistName(),null) :
                                HttpClient.getLastFMApiservice().getArtistInfo(request.getArtistName(),null);
                    }
                    @Override
                    public ObservableSource<String> apply(Boolean aBoolean) {
                        return getObservable(request)
                                .map(responseBody -> parseLastFMNetworkImageUrl(request,responseBody));
                    }
                }).firstElement().toObservable());
    }

    private String parseLastFMNetworkImageUrl(NewUriRequest request, ResponseBody body) throws IOException{
        String imageUrl = "";
        String bodyString = body.string();
        if(request.getSearchType() == ImageUriRequest.URL_ALBUM){
            LastFmAlbum lastFmAlbum = new Gson().fromJson(bodyString, LastFmAlbum.class);
            imageUrl = ImageUriUtil.getLargestAlbumImageUrl(lastFmAlbum.getAlbum().getImage());
        }else if(request.getSearchType() == ImageUriRequest.URL_ARTIST){
            LastFmArtist lastFmArtist = new Gson().fromJson(bodyString, LastFmArtist.class);
            imageUrl = ImageUriUtil.getLargestArtistImageUrl(lastFmArtist.getArtist().getImage());
        }
        if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
            SPUtil.putValue(App.getContext(),SPUtil.COVER_KEY.COVER_NAME,request.getLastFMKey(),imageUrl);
        }
        return imageUrl;
    }

    //网易
    private Observable<String> getNeteaseNetworkThumbObservable(NewUriRequest request){
        return Observable.concat(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                String imageUrl = SPUtil.getValue(App.getContext(),SPUtil.COVER_KEY.COVER_NAME,request.getNeteaseKey(),"");
                if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
                    observer.onNext(imageUrl);
                }
                observer.onComplete();
            }},Observable.just(isAutoDownloadCover() && !TextUtils.isEmpty(request.getNeteaseKey()))
                .filter(aBoolean -> aBoolean)
                .flatMap(aBoolean -> HttpClient.getNeteaseApiservice()
                        .getNeteaseSearch(request.getNeteaseKey(), 0, 1, request.getSearchType())
                        .map(responseBody -> parseNeteaseNetworkImageUrl(request, responseBody))
                        .firstElement().toObservable()));
    }

    @Nullable
    private String parseNeteaseNetworkImageUrl(NewUriRequest request, ResponseBody body) throws IOException {
        String imageUrl = "";
        if (request.getSearchType() == TYPE_NETEASE_SONG) {
            //搜索的是歌曲
            NSongSearchResponse response = new Gson().fromJson(body.string(), NSongSearchResponse.class);
            imageUrl = response.result.songs.get(0).album.picUrl;
        } else if (request.getSearchType() == TYPE_NETEASE_ALBUM) {
            //搜索的是专辑
            NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
            imageUrl = response.result.albums.get(0).picUrl;
        } else if (request.getSearchType() == TYPE_NETEASE_ARTIST) {
            //搜索的是艺术家
            NArtistSearchResponse response = new Gson().fromJson(body.string(), NArtistSearchResponse.class);
            imageUrl = response.getResult().getArtists().get(0).getPicUrl();
        }
        if(!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))){
            SPUtil.putValue(App.getContext(),SPUtil.COVER_KEY.COVER_NAME,request.getNeteaseKey(),imageUrl);
        }
        return imageUrl;
    }

    protected Observable<Bitmap> getThumbBitmapObservable(NewUriRequest request) {
        return getCoverObservable(request)
                .flatMap((Function<String, ObservableSource<Bitmap>>) url -> Observable.create(e -> {
                    Uri imageUri = !TextUtils.isEmpty(url) ? Uri.parse(url) : Uri.EMPTY;
                    ImageRequest imageRequest =
                            ImageRequestBuilder.newBuilderWithSource(imageUri)
                                    .setResizeOptions(new ResizeOptions(mConfig.getWidth(),mConfig.getHeight()))
                                    .build();
                    DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,App.getContext());
                    dataSource.subscribe(new BaseBitmapDataSubscriber() {
                        @Override
                        protected void onNewResultImpl(Bitmap bitmap) {
                            Bitmap result = copy(bitmap);
                            if(result == null) {
                                result = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.album_empty_bg_day);
                            }
                            e.onNext(result);
                            e.onComplete();
                        }

                        @Override
                        protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                            e.onError(dataSource.getFailureCause());
                        }
                    }, CallerThreadExecutor.getInstance());
                }));
    }

    /**
     * 是否下载封面
     * @return
     */
    protected boolean isAutoDownloadCover() {
        Context context = App.getContext();
        return context.getString(R.string.always).equals(AUTO_DOWNLOAD_ALBUM) || (context.getString(R.string.wifi_only).equals(AUTO_DOWNLOAD_ALBUM) && isWifi(context));
    }


    private static final String[] FALLBACKS = {"cover.jpg", "album.jpg", "folder.jpg"};
    private File fallback(Song song) {
        File parent = new File(song.getUrl()).getParentFile();

        File same = new File(parent,song.getArtist() + " - " + song.getTitle() + ".jpg");
        if(same.exists())
            return same;

        for (String fallback : FALLBACKS) {
            File cover = new File(parent, fallback);
            if (cover.exists()) {
                return cover;
            }
        }
        return null;
    }
}
