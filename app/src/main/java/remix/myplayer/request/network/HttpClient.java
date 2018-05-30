package remix.myplayer.request.network;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
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
    private static final String LASTFM_BASE_URL = "http://ws.audioscrobbler.com/2.0/";
    private static final long TIMEOUT = 5000;

    private ApiService mNeteaseApi;
    private ApiService mKuGouApi;
    private ApiService mLastfmApi;

    public static HttpClient getInstance(){
        return SingletonHolder.mInstance;
    }

    public static ApiService getNeteaseApiservice(){
        return getInstance().mNeteaseApi;
    }

    public static ApiService getKuGouApiservice(){
        return getInstance().mKuGouApi;
    }

    public static ApiService getLastFMApiservice(){
        return getInstance().mLastfmApi;
    }

    private HttpClient(){
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();

        mNeteaseApi = retrofitBuilder
                .baseUrl(NETEASE_BASE_URL)
                .client(new OkHttpClient.Builder()
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
                        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        mLastfmApi = retrofitBuilder
                .baseUrl(LASTFM_BASE_URL)
                .client(new OkHttpClient.Builder()
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

//    private Interceptor TEMP = chain -> {
//        Request request = chain.request();//获取请求
//        if(!Util.isNetWorkConnected()){
//            //没有网络的时候从缓存读取数据
//            String key = Util.hashKeyForDisk(request.url().toString());
//            InputStream cacheInput = DiskCache.getHttpDiskCache().get(key).getInputStream(0);
//            byte[] cacheBytes = new byte[cacheInput.available()];
//            cacheInput.close();
//
//            return new Response.Builder()
//                    .removeHeader("Pragma")
//                    .body(ResponseBody.create(MediaType.importM3UFile("text/plain;charset=UTF-8"), cacheBytes))
//                    .request(request)
//                    .protocol(Protocol.HTTP_1_1)
//                    .code(200)
//                    .build();
//        }else{
//            //将数据缓存
//            Response originalResponse = chain.proceed(request);
//            ResponseBody responseBody = originalResponse.body();
//            byte[] responseBytes = responseBody.bytes();
//            MediaType mediaType = responseBody.contentType();
//
//            try {
//                String key = Util.hashKeyForDisk(request.url().toString());
//                DiskLruCache.Editor editor = DiskCache.getHttpDiskCache().edit(key);
//                if(editor != null){
//                    OutputStream outputStream = editor.newOutputStream(0);
//                    outputStream.write(responseBytes);
//                    outputStream.flush();
//                    outputStream.close();
//                    editor.commit();
//                }
//            } catch (Exception e){
//                e.printStackTrace();
//            }
//
//            return originalResponse.newBuilder()
//                    .removeHeader("Pragma")
//                    .removeHeader("Cache-Control")
//                    .request(request)
//                    .header("Cache-Control", "public, max-age=" + 31536000)
//                    .body(ResponseBody.create(mediaType, responseBytes))
//                    .build();
//        }
//    };

//    private Interceptor REWRITE_RESPONSE_INTERCEPTOR = chain -> {
//        Request request = chain.request();//获取请求
//
//        //将数据缓存
//        Response originalResponse = chain.proceed(request);
//        ResponseBody responseBody = originalResponse.body();
//        byte[] responseBytes = responseBody.bytes();
//        MediaType mediaType = responseBody.contentType();
//
//        try {
//            String key = Util.hashKeyForDisk(request.url().toString());
//            DiskLruCache.Editor editor = DiskCache.getHttpDiskCache().edit(key);
//            if(editor != null){
//                OutputStream outputStream = editor.newOutputStream(0);
//                outputStream.write(responseBytes);
//                outputStream.flush();
//                outputStream.close();
//                editor.commit();
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        Response newResponse = originalResponse.newBuilder()
//                .removeHeader("Pragma")
//                .removeHeader("Cache-Control")
//                .request(request)
//                .header("Cache-Control", "public, max-age=" + 31536000)
//                .body(ResponseBody.create(mediaType, responseBytes))
//                .build();
//        return newResponse;
//    };

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
