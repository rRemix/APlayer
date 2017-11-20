package remix.myplayer.model.network;

import android.content.Context;

import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Remix on 2017/11/20.
 */

public class RequestManager {
    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";
    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";

    private ApiService mNeteaseApi;
    private ApiService mKuGouApi;

    private static RequestManager mInstance;
    public static RequestManager getInstance(){
        return SingletonHolder.mInstance;
    }
    private RequestManager(){
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

    private <T> void addRequest(Context context,Map<String,String> params){

    }

    private static class SingletonHolder{
        public static RequestManager mInstance = new RequestManager();
    }
}
