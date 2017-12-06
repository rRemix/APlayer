package remix.myplayer.request.network;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import remix.myplayer.APlayerApplication;
import remix.myplayer.lyric.HttpHelper;
import remix.myplayer.misc.cache.DiskCache;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Remix on 2017/11/20.
 */

public class HttpClient implements HttpHelper {
    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";
    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";
    private static final long TIMEOUT = 1000;

    private ApiService mNeteaseApi;
    private ApiService mKuGouApi;

    public static HttpClient getInstance(){
        return SingletonHolder.mInstance;
    }

    public static ApiService getNeteaseApiservice(){
        return getInstance().mNeteaseApi;
    }

    public static ApiService getKuGouApiservice(){
        return getInstance().mKuGouApi;
    }

    private HttpClient(){
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();

        mNeteaseApi = retrofitBuilder
                .baseUrl(NETEASE_BASE_URL)
                .client(new OkHttpClient.Builder()
                        .cache(createDefaultCache(APlayerApplication.getContext()))
                        .addInterceptor(createCacheControlInterceptor())
                        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        mKuGouApi = retrofitBuilder
                .baseUrl(KUGOU_BASE_URL)
                .client(new OkHttpClient.Builder()
                        .cache(createDefaultCache(APlayerApplication.getContext()))
                        .addInterceptor(createCacheControlInterceptor())
                        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);
    }

    @Nullable
    private Cache createDefaultCache(Context context) {
        File cacheDir = DiskCache.getDiskCacheDir(context,"okhttp");
        if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
            return new Cache(cacheDir, 1024 * 1024 * 10);
        }
        return null;
    }

    private Interceptor createCacheControlInterceptor() {
        return chain -> {
            Request modifiedRequest = chain.request().newBuilder()
//                    .cacheControl(new CacheControl.Builder()
//                            .maxAge(31536000, TimeUnit.SECONDS)
//                            .maxStale(31536000,TimeUnit.SECONDS)
//                            .build())
                    .removeHeader("pragma")
                    .removeHeader("Cache-Control")
                    .addHeader("Cache-Control", "max-age=" + 31536000)
                    .addHeader("Cache-Control","max-stale=" + 31536000)
                    .build();
            return chain.proceed(modifiedRequest);
        };
    }

    @Override
    public Observable<ResponseBody> getNeteaseSearch(String key, int offset, int limit, int type) {
        return mNeteaseApi.getNeteaseSearch(key,offset,limit,type);
    }

    @Override
    public Observable<ResponseBody> getNeteaseLyric(int song_id) {
        return mNeteaseApi.getNeteaseLyric("pc",song_id,-1,-1,-1);
    }

    @Override
    public Observable<ResponseBody> getKuGouSearch(String keyword, long duration, String hash) {
        return mKuGouApi.getKuGouSearch(1,"yes","pc",keyword,duration,"");
    }

    @Override
    public Observable<ResponseBody> getKuGouLyric(int id,String accessKey) {
        return mKuGouApi.getKuGouLyric(1,"pc","lrc","utf8",id,accessKey);
    }

    private static class SingletonHolder{
        static HttpClient mInstance = new HttpClient();
    }
}
