package remix.myplayer.request.network;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import remix.myplayer.lyric.HttpHelper;
import remix.myplayer.model.netease.NSongSearchResponse;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Remix on 2017/11/20.
 */

public class HttpClient implements HttpHelper {
    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";
    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";

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
                .client(new OkHttpClient.Builder().build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        mKuGouApi = retrofitBuilder
                .baseUrl(KUGOU_BASE_URL)
                .client(new OkHttpClient.Builder().build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);
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
    public Observable<ResponseBody> getKuGouSearch(String keyword, int duration, String hash) {
        return mKuGouApi.getKuGouSearch(1,"yes","pc",keyword,duration,"");
    }

    @Override
    public Observable<ResponseBody> getKuGouLyric(int id,String accessKey) {
        return mKuGouApi.getKuGouLyric(1,"pc","lrc","utf8",id,accessKey);
    }

    @Override
    public Observable<NSongSearchResponse> getNeteaseSearch2(String key, int offset, int limit, int type) {
        return mNeteaseApi.getNeteaseSearch2(key,offset,limit,type);
    }

    private static class SingletonHolder{
        static HttpClient mInstance = new HttpClient();
    }
}
