package remix.myplayer.request.network

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody
import remix.myplayer.bean.github.Release
import remix.myplayer.request.network.ApiService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Remix on 2017/11/20.
 */
class HttpClient private constructor() {
  private val neteaseApi: ApiService
  private val kuGouApi: ApiService
  private val qqApi: ApiService
  private val lastfmApi: ApiService
  private val githubApi: ApiService

  //    private static OkHttpClient sOkHttpClient;
  //    private static SSLSocketFactory mSSLSocketFactory;
  //    public static SSLSocketFactory getSSLSocketFactory(){
  //        if(mSSLSocketFactory == null){
  //            try {
  //                // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
  //                final X509TrustManager trustAllCert =
  //                        new X509TrustManager() {
  //                            @Override
  //                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
  //                                return new java.security.cert.X509Certificate[]{};
  //                            }
  //                        };
  //                mSSLSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        }
  //        return mSSLSocketFactory;
  //    }
  //
  //    public synchronized static OkHttpClient getOkHttpClient(){
  //        if (sOkHttpClient == null) {
  //            OkHttpClient.Builder builder = new OkHttpClient.Builder();
  //            try {
  //                // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
  //                final X509TrustManager trustAllCert =
  //                        new X509TrustManager() {
  //                            @Override
  //                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
  //                                return new java.security.cert.X509Certificate[]{};
  //                            }
  //                        };
  //                builder.sslSocketFactory(getSSLSocketFactory(), trustAllCert);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //            builder.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .cache(createDefaultCache(App.getContext()));
  //            sOkHttpClient = builder.build();
  //        }
  //        return sOkHttpClient;
  //    }
  fun getNeteaseSearch(key: String?, offset: Int, limit: Int, type: Int): Observable<ResponseBody> {
    return neteaseApi.getNeteaseSearch(key, offset, limit, type)
  }

  fun getNeteaseLyric(song_id: Int): Observable<ResponseBody> {
    return neteaseApi.getNeteaseLyric("pc", song_id, -1, -1, -1)
  }

  fun getKuGouSearch(keyword: String?, duration: Long, hash: String?): Observable<ResponseBody> {
    return kuGouApi.getKuGouSearch(1, "yes", "pc", keyword, duration, "")
  }

  fun getKuGouLyric(id: Int, accessKey: String?): Observable<ResponseBody> {
    return kuGouApi.getKuGouLyric(1, "pc", "lrc", "utf8", id, accessKey)
  }

  fun getQQSearch(key: String?): Observable<ResponseBody> {
    return qqApi.getQQSearch(1, key, "json")
  }

  fun getQQLyric(songmid: String?): Observable<ResponseBody> {
    return qqApi.getQQLyric(songmid, 5381, "json", 1)
  }

  fun getAlbumInfo(albumName: String?, artistName: String?, lang: String?): Observable<ResponseBody> {
    return lastfmApi.getAlbumInfo(albumName, artistName, lang)
  }

  fun getArtistInfo(artistName: String?, lang: String?): Observable<ResponseBody> {
    return lastfmApi.getArtistInfo(artistName, lang)
  }

  fun getLatestRelease(owner: String, repo: String): Single<Release> {
    return githubApi.getLatestRelease(owner, repo)
  }

  private object SingletonHolder {
    val instance = HttpClient()
  }

  companion object {
    @JvmStatic
    fun getInstance(): HttpClient = SingletonHolder.instance

    private const val NETEASE_BASE_URL = "http://music.163.com/api/"
    private const val KUGOU_BASE_URL = "http://lyrics.kugou.com/"
    private const val QQ_BASE_URL = "https://c.y.qq.com/"
    private const val LASTFM_BASE_URL = "http://ws.audioscrobbler.com/2.0/"
    private const val GITHUB_BASE_URL = "https://api.github.com/"
  }

  init {
    val retrofitBuilder = Retrofit.Builder()
    val okHttpClient = OkHttpHelper.okHttpClient
    neteaseApi = retrofitBuilder
        .baseUrl(NETEASE_BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)
    kuGouApi = retrofitBuilder
        .baseUrl(KUGOU_BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)
    qqApi = retrofitBuilder
        .baseUrl(QQ_BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)
    lastfmApi = retrofitBuilder
        .baseUrl(LASTFM_BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)
    githubApi = retrofitBuilder
        .baseUrl(GITHUB_BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ApiService::class.java)
  }
}